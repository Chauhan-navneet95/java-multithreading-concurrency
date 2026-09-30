# Java Concurrency: `wait()`, `notify()`, and `notifyAll()`

## 1. `join()` vs `wait()`

### `join()`

`threadB.join()` means:

> The **current thread** waits until **thread B terminates**.

Example:

```java
Thread threadB = new Thread(() -> {
    // work
});

threadB.start();
threadB.join();

// Current thread continues after threadB terminates
```

Think:

```text
Current Thread
      |
      | join(threadB)
      ↓
   WAITING
      |
      | threadB terminates
      ↓
  continues
```

### `wait()`

`wait()` is different.

It means:

> The current thread is waiting for some **shared state/condition** to become suitable for it to continue.

And critically:

> `wait()` releases the monitor/lock that the thread currently owns.

Typical pattern:

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }

    // condition is now true
}
```

Think:

```text
SHARED STATE
     ↓
Is condition true?
   /       \
 NO         YES
 |           |
wait()      work
 |
release lock
 |
WAITING
```

---

# 2. The Core Mental Model

The easiest way to understand `wait()`/`notify()` is:

> **Threads wait because some condition is not true. Another thread changes the shared state and signals that waiting threads should check again.**

For example:

```text
             Shared State
                  |
          "Is data available?"
             /          \
           NO            YES
           |              |
         wait()           work
           |
      release lock
           |
        WAITING
           ↑
           |
     notify/notifyAll
           |
 another thread changes
      shared state
```

The important thing is that `notify()` does **not** directly transfer execution to the waiting thread.

---

# 3. `wait()` Must Be Called While Holding the Monitor

This is mandatory:

```java
synchronized (lock) {
    lock.wait();
}
```

or:

```java
synchronized void method() throws InterruptedException {
    wait();
}
```

If you call `wait()` without owning that object's monitor:

```java
lock.wait(); // WRONG if lock is not owned
```

you get:

```text
IllegalMonitorStateException
```

The same rule applies to:

```java
notify();
notifyAll();
```

You must own the same object's monitor.

---

# 4. Simple Two-Thread Example

```java
class Shared {

    synchronized void doSomething() throws InterruptedException {
        System.out.println(
            Thread.currentThread().getName() + " is waiting..."
        );

        wait();

        System.out.println(
            Thread.currentThread().getName() + " woke up!"
        );
    }

    synchronized void wakeSomeone() {

        System.out.println(
            Thread.currentThread().getName() + " is notifying..."
        );

        notify();
    }
}
```

Suppose:

```text
Thread T1 → doSomething()
Thread T2 → wakeSomeone()
```

### Step 1 — T1 enters `doSomething()`

Because the method is synchronized:

```text
T1 acquires Shared's monitor
```

Then:

```java
wait();
```

is executed.

### Step 2 — T1 calls `wait()`

T1:

```text
WAITING
```

And, critically:

```text
T1 releases Shared's monitor
```

This is the special behavior of `wait()`.

Now another thread can enter a synchronized method on `Shared`.

---

# 5. T2 Calls `notify()`

T2 enters:

```java
synchronized void wakeSomeone()
```

So:

```text
T2 acquires Shared's monitor
```

Then:

```java
notify();
```

is called.

T1 is now eligible to wake up.

But **T1 does not immediately run**.

T2 still owns the monitor.

So:

```text
T1 → eligible to wake
T2 → still owns lock
```

T2 eventually leaves the synchronized method.

The monitor becomes available.

T1 can then reacquire the monitor and continue after:

```java
wait();
```

So the flow is:

```text
T1
 |
 | synchronized
 ↓
acquires lock
 |
 | wait()
 ↓
WAITING
 |
 | releases lock
 ↓
monitor becomes available
 |
 |
T2
 |
 | synchronized
 ↓
acquires lock
 |
 | notify()
 ↓
T1 becomes eligible
 |
 | T2 still owns lock
 ↓
T2 exits synchronized
 |
 | releases lock
 ↓
T1 reacquires lock
 |
 ↓
continues after wait()
```

---

# 6. `notify()` Does NOT Mean "Run This Thread Now"

This is a very important distinction.

When you call:

```java
notify();
```

you are **not** saying:

> "Run the waiting thread immediately."

You are saying approximately:

> "One thread waiting on this object's monitor may now become eligible to continue."

The notified thread still needs to reacquire the monitor.

Therefore:

```text
notify()
   ↓
waiting thread becomes eligible
   ↓
must reacquire monitor
   ↓
only then can it continue
```

---

# 7. `notify()` vs `notifyAll()`

## `notify()`

Wakes/signals **one arbitrary waiting thread**.

```java
notify();
```

There is no:

```java
notify(threadA);
```

You cannot choose a particular waiting thread.

---

## `notifyAll()`

Makes **all threads waiting on that object's monitor** eligible to wake.

```java
notifyAll();
```

But this does **not** mean all of them execute simultaneously.

They still need to acquire the same monitor.

For example:

```text
T1 ── WAITING
T2 ── WAITING
T3 ── WAITING

        |
        | notifyAll()
        ↓

T1 ── eligible
T2 ── eligible
T3 ── eligible

        ↓

Only one can acquire the monitor at a time.

        ↓

T1 → acquires lock → runs
T2 → waits for lock
T3 → waits for lock
```

So:

> `notifyAll()` wakes all waiting threads conceptually, but they compete for the monitor one at a time.

---

# 8. Why `while`, Not `if`?

The standard pattern is:

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }

    // proceed
}
```

Not:

```java
synchronized (lock) {
    if (!condition) {
        lock.wait();
    }

    // proceed
}
```

Why?

Because waking up does **not** guarantee that the condition is true.

A thread should interpret waking up as:

> "Something may have changed. Let me check the condition again."

Therefore:

```text
wait()
  ↓
wake up
  ↓
reacquire lock
  ↓
check condition again
  ↓
condition true?
  ├── NO → wait again
  └── YES → continue
```

There can also be **spurious wakeups**, where a waiting thread returns from `wait()` without the condition becoming true.

The `while` loop protects against this.

---

# 9. Producer-Consumer Example

A very common use of `wait()` and `notifyAll()` is a producer-consumer buffer.

```java
class Buffer {

    private Integer value;

    public synchronized void put(int value)
            throws InterruptedException {

        while (this.value != null) {
            wait();
        }

        this.value = value;

        System.out.println("Produced: " + value);

        notifyAll();
    }

    public synchronized int get()
            throws InterruptedException {

        while (this.value == null) {
            wait();
        }

        int result = this.value;

        this.value = null;

        System.out.println("Consumed: " + result);

        notifyAll();

        return result;
    }
}
```

The buffer can hold one value.

There are two conditions:

```text
Producer:
    buffer must be EMPTY

Consumer:
    buffer must be NON-EMPTY
```

---

# 10. Producer Waiting

Suppose:

```text
buffer = 10
```

Producer wants to put:

```text
20
```

Producer enters:

```java
public synchronized void put(int value)
```

It owns the buffer monitor.

Then:

```java
while (this.value != null) {
    wait();
}
```

Since:

```text
value != null
```

the producer waits.

Calling:

```java
wait();
```

does two things:

```text
1. Producer enters WAITING
2. Producer releases the buffer's monitor
```

This is why the consumer can now enter `get()`.

---

# 11. Consumer Wakes the Producer

Consumer enters:

```java
public synchronized int get()
```

It acquires the monitor.

It consumes:

```text
10
```

Then:

```java
this.value = null;
```

Now the buffer is empty.

Consumer calls:

```java
notifyAll();
```

The producer becomes eligible to wake.

But consumer still owns the monitor until `get()` exits.

After consumer exits:

```text
Consumer releases monitor
```

Producer can reacquire it.

Then producer wakes up and checks:

```java
while (this.value != null)
```

Now:

```text
value == null
```

So the condition is satisfied.

Producer proceeds:

```java
this.value = 20;
```

---

# 12. The Most Important Mental Model

Keep this model in your head:

```text
                 SHARED STATE
                     |
                     ↓
             Is condition true?
                /          \
              NO            YES
              |               |
           wait()            work
              |
              ↓
       release monitor
              |
              ↓
           WAITING
              |
              |
     another thread changes
          shared state
              |
              ↓
       notify / notifyAll
              |
              ↓
     waiting thread becomes
          eligible
              |
              ↓
       reacquire monitor
              |
              ↓
       check condition again
              |
        +-----+------+
        |            |
       NO           YES
        |            |
      wait()        work
```

This is the essence of `wait()`/`notify()`.

---

# 13. Thread State Flow

A useful way to visualize the states:

```text
              wait()
RUNNABLE ----------------→ WAITING
                              |
                              | notify()
                              ↓
                         eligible to run
                              |
                              ↓
                    tries to reacquire lock
                              |
                              ↓
                         BLOCKED
                              |
                              | lock acquired
                              ↓
                          RUNNABLE
```

More precisely, after `notify()` the waiting thread is no longer simply waiting for the notification, but it still cannot continue until it successfully reacquires the monitor.

If another thread still owns the monitor, it can spend time in `BLOCKED`.

---

# 14. `wait()` vs `sleep()`

This distinction is extremely important.

## `wait()`

```java
synchronized (lock) {
    lock.wait();
}
```

- Releases the monitor.
- Used for coordination.
- Usually associated with a condition/shared state.
- Requires owning the monitor.
- Can be awakened with `notify()` / `notifyAll()`.

## `sleep()`

```java
Thread.sleep(1000);
```

- Does **not** release a monitor that the thread owns.
- Used for timing/delay.
- Does not require synchronization.
- Does not need `notify()`.

Example:

```java
synchronized (lock) {
    Thread.sleep(5000);
}
```

The thread sleeps for 5 seconds **while still holding `lock`**.

Compare:

```java
synchronized (lock) {
    lock.wait();
}
```

Here the thread waits and **releases `lock`**.

---

# 15. `join()` vs `wait()` vs `sleep()`

A useful comparison:

| Mechanism | Why use it? | Releases monitor? |
|---|---|---|
| `join()` | Wait for another thread to terminate | Not as a general monitor-release mechanism |
| `wait()` | Wait for a condition/shared-state change | **Yes** |
| `sleep()` | Delay execution for a period | **No** |

Mental model:

```text
join()
→ "Wait until THAT thread finishes."

wait()
→ "Wait until THIS shared condition becomes true."

sleep()
→ "Pause me for THIS amount of time."
```

---

# 16. One More Important Detail: `notify()` Is Not a Lock Release

Consider:

```java
synchronized void update() {

    sharedState = READY;

    notify();

    // more work
}
```

After:

```java
notify();
```

the waiting thread does **not** immediately acquire the lock.

The current thread still owns it.

So this is possible:

```text
T1 owns lock
 |
 | change state
 |
 | notify()
 |
 | more work
 |
 | more work
 |
 ↓
exit synchronized
 |
 ↓
release lock
 |
 ↓
T2 can acquire lock
```

This is one of the most commonly misunderstood parts of `notify()`.

---

# 17. The Best Practical Pattern

When using intrinsic monitors, the general pattern is:

```java
synchronized (lock) {

    while (!condition) {
        lock.wait();
    }

    // condition is satisfied
}
```

And the thread changing the condition does:

```java
synchronized (lock) {

    // change shared state

    lock.notifyAll();
}
```

The important relationship is:

```text
WAITING THREAD
    |
    | waits because condition is false
    ↓
wait()

                 SHARED STATE

    ↑
    |
    | another thread changes condition
    |
NOTIFYING THREAD
    |
    | notifyAll()
    ↓

WAITING THREADS
    |
    | wake and reacquire lock
    ↓
check condition again
```

The notification is essentially a **signal to re-check**, not a command to immediately execute.

---

# 18. Final Mental Model

If you remember only one thing, remember this:

> **`wait()` means: "I cannot proceed right now. I will release the lock and sleep until another thread signals that the shared state may have changed."**

> **`notify()` means: "One waiting thread may now wake up and try to reacquire this monitor."**

> **`notifyAll()` means: "All threads waiting on this monitor may wake up and compete to reacquire it."**

And the canonical pattern is:

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }

    // proceed
}
```

with:

```java
synchronized (lock) {
    // change shared state
    lock.notifyAll();
}
```

The deepest mental model is therefore:

```text
        CONDITION
           |
     +-----+-----+
     |           |
    false       true
     |           |
   wait()       work
     |
 release lock
     |
 WAITING
     |
 notify/notifyAll
     |
 reacquire lock
     |
 check condition again
```
