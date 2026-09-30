# Java Concurrency — `java.util.concurrent.locks`

## Why does Java need `java.util.concurrent.locks` if `synchronized` already exists?

The key is **not** to start with `ReentrantLock` APIs. Start with why Java needed another locking mechanism when `synchronized` already existed.

---

## 1. What problem does a lock solve?

```java
class Counter {
    private int count;

    void increment() {
        count++;
    }
}
```

`count++` is not one atomic operation:

```text
read count
   ↓
add 1
   ↓
write count
```

Two threads can interfere.

A lock provides **mutual exclusion**:

```text
        LOCK
         ↓
Thread A → critical section
         ↓
       unlock

Thread B → waits
         ↓
Thread A unlocks
         ↓
Thread B → critical section
```

Java gives us:

```java
synchronized
```

and it remains an excellent way to provide mutual exclusion.

---

## 2. Why did Java introduce `java.util.concurrent.locks`?

Because `synchronized` is intentionally **simple and restrictive**.

```java
synchronized void increment() {
    count++;
}
```

This essentially says:

> Only one thread can execute this synchronized region at a time.

But some concurrent programs need **more control over how threads acquire and release locks**.

That's where:

```java
java.util.concurrent.locks
```

comes in.

Think of it as:

```text
synchronized
     ↓
simple built-in locking

java.util.concurrent.locks
     ↓
programmable / configurable locking
```

---

## 3. The biggest difference

Compare:

```java
synchronized (lock) {
    doSomething();
}
```

with:

```java
lock.lock();

try {
    doSomething();
} finally {
    lock.unlock();
}
```

For basic mutual exclusion, the second is often unnecessary.

But it gives you capabilities that `synchronized` does not provide as directly.

For example:

```java
if (lock.tryLock()) {
    try {
        doSomething();
    } finally {
        lock.unlock();
    }
}
```

Meaning:

> Try to acquire the lock. If I can't get it immediately, don't block.

That's a major capability.

---

## 4. `synchronized` basically says "wait"

Suppose:

```java
synchronized (lock) {
    expensiveOperation();
}
```

Thread B reaches the same lock while Thread A owns it.

Thread B has to wait for the monitor.

With `ReentrantLock`:

```java
if (lock.tryLock()) {
    try {
        expensiveOperation();
    } finally {
        lock.unlock();
    }
} else {
    // Couldn't get lock
    doSomethingElse();
}
```

Now you have a choice.

That's one of the fundamental reasons `Lock` exists.

---

# 5. `ReentrantLock`

The most important implementation you'll encounter is:

```java
ReentrantLock
```

Example:

```java
import java.util.concurrent.locks.ReentrantLock;

class Counter {

    private int count;

    private final ReentrantLock lock = new ReentrantLock();

    void increment() {

        lock.lock();

        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
}
```

Conceptually:

```text
lock.lock()
     ↓
acquire lock
     ↓
critical section
     ↓
lock.unlock()
     ↓
release lock
```

This provides essentially the same basic mutual-exclusion guarantee as:

```java
synchronized
```

---

# 6. Why bother with `ReentrantLock`?

There are four major capabilities to understand.

### 1. `tryLock()`

Don't wait indefinitely:

```java
if (lock.tryLock()) {
    try {
        // got lock
    } finally {
        lock.unlock();
    }
}
```

### 2. `tryLock(timeout)`

Wait for a limited amount of time:

```java
if (lock.tryLock(2, TimeUnit.SECONDS)) {
    try {
        // got lock
    } finally {
        lock.unlock();
    }
}
```

Meaning:

> I'll wait up to 2 seconds. If I can't get it, I'll give up.

### 3. Interruptible lock acquisition

```java
lock.lockInterruptibly();
```

A thread waiting for the lock can be interrupted.

```text
Thread waiting for lock
        |
        | interrupt()
        ↓
Thread stops waiting
```

This is useful for cancellable operations.

### 4. Multiple `Condition`s

This is one of the most important features because it connects directly to `wait()` / `notify()`.

---

# 7. What does "reentrant" mean?

A `ReentrantLock` allows the **same thread** to acquire the lock multiple times.

```java
ReentrantLock lock = new ReentrantLock();

void methodA() {

    lock.lock();

    try {
        methodB();
    } finally {
        lock.unlock();
    }
}

void methodB() {

    lock.lock();

    try {
        // work
    } finally {
        lock.unlock();
    }
}
```

Conceptually:

```text
Thread A owns lock

hold count = 1

lock.lock()

hold count = 2

unlock()

hold count = 1

unlock()

hold count = 0
lock released
```

That's why it is called **ReentrantLock**.

---

# 8. `synchronized` is ALSO reentrant

This is important.

Reentrancy is **not** the reason `ReentrantLock` exists.

This works:

```java
synchronized void methodA() {
    methodB();
}

synchronized void methodB() {
    // works
}
```

The same thread can enter both synchronized methods on the same object.

So:

```text
Reentrant
    ≠
reason for ReentrantLock
```

The important part is the **extra lock-management capabilities**.

---

# 9. The biggest feature: `Condition`

Now connect this to:

```java
wait()
notify()
notifyAll()
```

With `synchronized`, you have one intrinsic monitor and its associated wait set.

For example:

```java
synchronized (lock) {

    while (!dataAvailable) {
        lock.wait();
    }

    // consume data
}
```

And:

```java
synchronized (lock) {

    dataAvailable = true;

    lock.notifyAll();
}
```

With the `Lock` API, this concept is separated.

You can create:

```java
Condition condition = lock.newCondition();
```

Then:

```java
condition.await();
```

instead of:

```java
wait();
```

And:

```java
condition.signal();
```

instead of:

```java
notify();
```

Or:

```java
condition.signalAll();
```

instead of:

```java
notifyAll();
```

---

# 10. Why is `Condition` useful?

Imagine a bounded queue.

There are two different reasons a thread might need to wait:

```text
Queue FULL
    ↓
Producer must wait

Queue EMPTY
    ↓
Consumer must wait
```

With `synchronized` / `wait()` you have essentially one wait set associated with the monitor.

With `Condition`, you can create two separate waiting conditions:

```java
ReentrantLock lock = new ReentrantLock();

Condition notEmpty = lock.newCondition();
Condition notFull = lock.newCondition();
```

Now:

```text
                  LOCK
                   |
          +--------+--------+
          |                 |
      notEmpty           notFull
          |                 |
      Consumers         Producers
       waiting            waiting
```

Producer:

```java
while (queue.isFull()) {
    notFull.await();
}
```

Consumer:

```java
while (queue.isEmpty()) {
    notEmpty.await();
}
```

When producer adds something:

```java
notEmpty.signal();
```

When consumer removes something:

```java
notFull.signal();
```

This gives you much finer control over coordination.

---

# 11. Why does this matter?

Suppose:

```text
10 consumers waiting
10 producers waiting
```

A producer adds an item.

You want to wake a **consumer**, because the queue is no longer empty.

With separate conditions:

```java
notEmpty.signal();
```

You're signaling the consumer waiting condition.

Conceptually:

```text
Queue was empty

Producer adds item

        ↓

notEmpty.signal()

        ↓

Consumer can proceed
```

---

# 12. Full Producer/Consumer example

```java
class Buffer {

    private final Queue<Integer> queue = new LinkedList<>();

    private final int capacity = 10;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public void put(int value) throws InterruptedException {

        lock.lock();

        try {

            while (queue.size() == capacity) {
                notFull.await();
            }

            queue.add(value);

            notEmpty.signal();

        } finally {
            lock.unlock();
        }
    }

    public int get() throws InterruptedException {

        lock.lock();

        try {

            while (queue.isEmpty()) {
                notEmpty.await();
            }

            int value = queue.remove();

            notFull.signal();

            return value;

        } finally {
            lock.unlock();
        }
    }
}
```

Conceptual mapping:

| Old mechanism | `Lock` mechanism |
|---|---|
| `synchronized` | `lock.lock()` |
| `wait()` | `condition.await()` |
| `notify()` | `condition.signal()` |
| `notifyAll()` | `condition.signalAll()` |
| monitor | explicit `Lock` |

---

# 13. Important: `Lock` does NOT simply mean "faster"

Don't build your mental model around:

> "`ReentrantLock` is faster than synchronized."

Modern JVMs have heavily optimized `synchronized`.

The reason to use `ReentrantLock` is primarily:

> **control and flexibility**, not simply speed.

For straightforward mutual exclusion:

```java
synchronized
```

is often the cleaner choice.

---

# 14. What else is in `java.util.concurrent.locks`?

Yes — `ReentrantLock` is **not the only lock-related class**.

Important types:

```text
java.util.concurrent.locks
│
├── Lock                  ← interface
│
├── ReentrantLock         ← common implementation
│
├── ReadWriteLock         ← interface
│
├── ReentrantReadWriteLock
│
├── StampedLock
│
└── Condition             ← coordination abstraction
```

---

# 15. `Lock` — interface

`Lock` is an interface:

```java
public interface Lock
```

It defines operations such as:

```text
lock()
unlock()
tryLock()
lockInterruptibly()
newCondition()
```

Common usage:

```java
Lock lock = new ReentrantLock();
```

The variable uses the abstraction while the actual implementation is `ReentrantLock`.

---

# 16. `ReentrantReadWriteLock`

Imagine:

```text
100 threads reading data
1 thread updating data
```

With a normal `ReentrantLock`:

```text
Reader 1 → lock
Reader 2 → waits
Reader 3 → waits
...
```

Only one reader at a time.

But readers aren't modifying anything.

`ReentrantReadWriteLock` provides:

```text
READ LOCK
WRITE LOCK
```

Multiple readers can hold the read lock simultaneously.

A writer requires exclusive access.

Conceptually:

```text
Reader A ─┐
Reader B ─┼── READ LOCK → all can proceed
Reader C ─┘

Writer ───── WRITE LOCK → exclusive
```

Example:

```java
ReentrantReadWriteLock rwLock =
        new ReentrantReadWriteLock();

rwLock.readLock().lock();

try {
    // read shared data
} finally {
    rwLock.readLock().unlock();
}
```

Writer:

```java
rwLock.writeLock().lock();

try {
    // modify shared data
} finally {
    rwLock.writeLock().unlock();
}
```

---

# 17. Why is `ReentrantReadWriteLock` useful?

Imagine:

```text
Configuration object
Cache
In-memory database
Metadata
Routing table
```

You have:

```text
10,000 reads
10 writes
```

A normal lock serializes everything:

```text
R1 → R2 → R3 → R4
```

A read/write lock can allow:

```text
R1 ─┐
R2 ─┤
R3 ─┼── concurrently
R4 ─┤
R5 ─┘
```

while writers remain exclusive.

That's a fundamentally different concurrency policy.

---

# 18. `StampedLock`

`StampedLock` is more advanced.

It provides:

```text
optimistic read
read lock
write lock
```

The interesting one is:

```java
long stamp = lock.tryOptimisticRead();
```

You optimistically read the data without taking a traditional read lock.

Then:

```java
if (!lock.validate(stamp)) {
    // Someone modified the data
    // retry using a real read lock
}
```

Conceptually:

```text
Optimistic read

        ↓

"I'll read without blocking."

        ↓

Was there a write during my read?

       /      NO   YES
     |     |
   valid  retry
```

This can be useful for heavily read-oriented data structures, but it is more complex and has important limitations.

You don't need to start with `StampedLock`.

---

# 19. Where does `AtomicInteger` fit?

You now have three different approaches.

## Atomic

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

Uses CAS-style atomic operations.

Think:

> **Can I perform this small state transition atomically without a traditional lock?**

## `synchronized`

```java
synchronized (lock) {
    count++;
}
```

Think:

> **I need mutual exclusion around this critical section.**

## `ReentrantLock`

```java
lock.lock();

try {
    count++;
} finally {
    lock.unlock();
}
```

Think:

> **I need mutual exclusion, but I also need more control over how the lock is acquired and how threads coordinate.**

---

# 20. The bigger Java concurrency toolbox

```text
                    JAVA CONCURRENCY
                          │
          ┌───────────────┼────────────────┐
          │               │                │
       Atomic          Locks          Coordination
          │               │                │
   AtomicInteger      synchronized       wait/notify
   AtomicLong         ReentrantLock      Condition
   AtomicReference    ReadWriteLock
                      StampedLock
```

Higher-level abstractions include:

```text
ExecutorService
Future
CompletableFuture
BlockingQueue
ConcurrentHashMap
Semaphore
CountDownLatch
CyclicBarrier
Phaser
```

These build more sophisticated concurrency behavior on top of lower-level primitives.

---

# 21. The most useful mental comparison

### `AtomicInteger`

```text
"I need to atomically change ONE piece of state."
```

Example:

```java
counter.incrementAndGet();
```

### `synchronized`

```text
"I need one thread at a time inside this critical section."
```

Example:

```java
synchronized (lock) {
    updateMultipleFields();
}
```

### `ReentrantLock`

```text
"I need one thread at a time,
BUT I need more control over locking."
```

Examples:

```text
tryLock()
tryLock(timeout)
lockInterruptibly()
Condition
```

---

# 22. Was `ReentrantLock` created because `synchronized` was bad?

**No.**

Think of it as:

```text
synchronized
    ↓
simple + safe + language-level
```

versus:

```text
Lock API
    ↓
more explicit + more configurable
```

If your requirement is simply:

```java
synchronized void update() {
    ...
}
```

there is often **no reason to replace it with `ReentrantLock`**.

But if you need:

```text
tryLock()
lockInterruptibly()
multiple Conditions
```

then `ReentrantLock` becomes useful.

---

# 23. Final mental model

Keep this mental model:

```text
                 MUTUAL EXCLUSION
                        │
              ┌─────────┴─────────┐
              │                   │
         synchronized          Lock API
              │                   │
        implicit monitor     explicit lock
              │                   │
       JVM manages it        YOU manage it
                                  │
                           ReentrantLock
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
                tryLock()    interruptible   Condition
```

**`synchronized` gives you a lock.**

**`java.util.concurrent.locks` gives you a lock abstraction that you can programmatically control.**

A useful learning progression is:

```text
synchronized
     ↓
wait / notify
     ↓
Lock / ReentrantLock
     ↓
Condition
     ↓
ReadWriteLock
     ↓
StampedLock
     ↓
higher-level concurrency utilities
```

Since you've already understood **CAS + AtomicInteger + `synchronized` + `wait/notify`**, the next useful topic is **`ReentrantLock` + `Condition`**.

That will make `Condition.await()` feel like the more flexible evolution of the `wait()` model.
