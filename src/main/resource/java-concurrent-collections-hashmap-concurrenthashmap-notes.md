# Java Concurrent Collections --- HashMap & ConcurrentHashMap Study Notes

> Practical notes from our study session. Focus: understanding the
> concepts and being able to reproduce the experiments in VS Code,
> rather than memorizing JDK source code.

------------------------------------------------------------------------

## 1. Where Concurrent Collections Fit

Java's concurrent collections live primarily in `java.util.concurrent`.

Important examples:

-   `ConcurrentHashMap`
-   `CopyOnWriteArrayList`
-   `CopyOnWriteArraySet`
-   `ConcurrentLinkedQueue`
-   `ConcurrentLinkedDeque`
-   `ConcurrentSkipListMap`
-   `ConcurrentSkipListSet`
-   `BlockingQueue` implementations such as `ArrayBlockingQueue` and
    `LinkedBlockingQueue`

Most of these APIs arrived with Java 5.

The important idea is that concurrent collections are not simply
"ordinary collections with `synchronized` added." They are designed
around particular concurrent access patterns and aim to provide thread
safety with better scalability.

------------------------------------------------------------------------

# 2. Why Were Concurrent Collections Created?

An ordinary collection such as:

``` java
Map<Integer, String> map = new HashMap<>();
```

does not provide thread-safe concurrent modification.

A simple synchronized wrapper is possible:

``` java
Map<Integer, String> map =
        Collections.synchronizedMap(new HashMap<>());
```

This provides thread-safe individual map operations, but uses a common
synchronization mechanism around the wrapped map. Under heavy
concurrency, this can become a scalability bottleneck.

`ConcurrentHashMap` was designed specifically for concurrent access.

Conceptually:

``` text
HashMap
    |
    +-- fast ordinary map
    +-- not thread-safe

synchronizedMap
    |
    +-- thread-safe
    +-- coarse-grained synchronization

ConcurrentHashMap
    |
    +-- thread-safe
    +-- designed for concurrent access
    +-- higher concurrency
    +-- atomic compound-map operations
```

------------------------------------------------------------------------

# 3. HashMap Refresher

## 3.1 Basic usage

``` java
Map<String, Integer> users = new HashMap<>();

users.put("Alice", 25);
users.put("Bob", 30);
users.put("Charlie", 35);

System.out.println(users);
System.out.println(users.get("Alice"));
```

A `HashMap` is based on hashing.

Conceptually:

``` text
key
 |
 v
hashCode()
 |
 v
hash / hash spreading
 |
 v
bucket index
 |
 v
table[index]
```

The underlying table is an array of buckets.

------------------------------------------------------------------------

# 4. HashMap Nodes and Buckets

Conceptually, a map entry can be viewed as:

``` text
Node<K,V>

+----------------+
| hash           |
| key            |
| value          |
| next ----------|----> another Node
+----------------+
```

A collision occurs when multiple keys map to the same bucket.

Example:

``` text
table[5]

   |
   v
Node(A)
   |
   v
Node(B)
   |
   v
Node(C)
```

Modern Java `HashMap` can transform a heavily-collided bucket into a
red-black tree.

So a simplified picture is:

``` text
HashMap
   |
   +-- table[]
         |
         +-- bucket
         |     |
         |     +--> Node --> Node --> Node
         |
         +-- bucket
         |
         +-- bucket
```

------------------------------------------------------------------------

# 5. `equals()` and `hashCode()`

For a key type:

``` java
class UserKey {

    private final int id;

    UserKey(int id) {
        this.id = id;
    }
}
```

When using objects as keys, understand the `equals()` / `hashCode()`
contract.

The fundamental rule:

``` text
if a.equals(b) == true
then
a.hashCode() == b.hashCode()
```

The reverse is not required:

``` text
same hashCode
    !=
same object/key
```

Different keys can have the same hash code. That is a hash collision.

------------------------------------------------------------------------

# 6. Why HashMap Is Unsafe for Concurrent Modification

Experiment:

``` java
import java.util.HashMap;
import java.util.Map;

public class HashMapRace {

    public static void main(String[] args)
            throws InterruptedException {

        Map<Integer, Integer> map = new HashMap<>();

        int LIMIT = 1_000_000;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < LIMIT; i++) {
                map.put(i, i);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < LIMIT; i++) {
                map.put(i, i);
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 0; i < LIMIT; i++) {
                map.put(i, i);
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println("Expected logical size: " + LIMIT);
        System.out.println("Actual size: " + map.size());
    }
}
```

Important:

All three threads insert the same keys:

``` text
Thread 1 -> 0 ... 999999
Thread 2 -> 0 ... 999999
Thread 3 -> 0 ... 999999
```

Therefore the correct logical number of distinct keys is **1,000,000**,
not 3,000,000.

The important lesson is not that a particular incorrect result is
guaranteed. The lesson is:

> `HashMap` provides no thread-safety guarantee for concurrent
> structural modification.

Its internal invariants can be violated when multiple threads modify it
concurrently.

------------------------------------------------------------------------

# 7. Three Map Implementations

## HashMap

``` java
Map<Integer, Integer> map =
        new HashMap<>();
```

-   Not thread-safe.
-   Good default for ordinary single-threaded access.
-   No synchronization overhead for normal use.

## Synchronized wrapper

``` java
Map<Integer, Integer> map =
        Collections.synchronizedMap(new HashMap<>());
```

-   Thread-safe for individual map operations.
-   Uses synchronization around the wrapped map.
-   Compound operations still need to be considered as a unit.

## ConcurrentHashMap

``` java
Map<Integer, Integer> map =
        new ConcurrentHashMap<>();
```

-   Thread-safe.
-   Designed for concurrent access.
-   Reads can proceed concurrently.
-   Updates use concurrency mechanisms appropriate to the operation.
-   Provides atomic compound operations such as `putIfAbsent`,
    `computeIfAbsent`, `compute`, and `merge`.

------------------------------------------------------------------------

# 8. Important Experiment: SynchronizedMap vs ConcurrentHashMap

For a simple workload like:

``` java
map.put(i, i);
```

both of these can produce the same correct result:

``` java
Collections.synchronizedMap(new HashMap<>())
```

and:

``` java
new ConcurrentHashMap<>()
```

That does **not** mean they have the same concurrency characteristics.

The distinction is about how operations are coordinated internally and
how much independent work can proceed concurrently.

------------------------------------------------------------------------

# 9. The Check-Then-Act Race

Consider:

``` java
if (!map.containsKey("user")) {
    map.put("user", value);
}
```

It looks correct, but the two operations are separate.

Possible execution:

``` text
Thread A                    Thread B

containsKey()
    |
 false
                            containsKey()
                                |
                               false

put()
                            put()
```

Both threads observed the key as absent.

This is called a **check-then-act race**.

------------------------------------------------------------------------

# 10. Making the Compound Operation Safe with synchronized

With a synchronized map, you can protect the entire compound operation:

``` java
synchronized (map) {

    if (!map.containsKey("user")) {
        map.put("user", value);
    }
}
```

Now the whole check-and-insert sequence is protected by one lock.

The downside is that we have to manually coordinate the compound
operation.

------------------------------------------------------------------------

# 11. ConcurrentHashMap: `putIfAbsent`

`ConcurrentHashMap` provides the operation directly:

``` java
map.putIfAbsent("user", value);
```

This means:

> Insert the mapping only if the key currently has no mapping.

Example:

``` java
ConcurrentHashMap<String, String> map =
        new ConcurrentHashMap<>();

String existing =
        map.putIfAbsent("user", "Thread-A");

if (existing == null) {
    System.out.println("I inserted the value");
} else {
    System.out.println("Already exists: " + existing);
}
```

Conceptually:

``` text
{}
 |
putIfAbsent("user", "A")
 |
{user=A}
```

A later:

``` java
putIfAbsent("user", "B")
```

does not replace `"A"`.

It returns the existing value.

------------------------------------------------------------------------

# 12. AtomicInteger Connection

The same conceptual idea appears with atomic variables.

Unsafe compound operation:

``` java
count++;
```

Conceptually:

``` text
read
 |
add
 |
write
```

Atomic alternative:

``` java
count.incrementAndGet();
```

Likewise:

``` java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

can become:

``` java
map.putIfAbsent(key, value);
```

The API exposes the useful state transition as an atomic map operation.

------------------------------------------------------------------------

# 13. `computeIfAbsent`

A common cache pattern:

``` java
User user = cache.get(userId);

if (user == null) {
    user = loadFromDatabase(userId);
    cache.put(userId, user);
}
```

Under concurrency, many threads can observe the cache miss and perform
the expensive operation.

`ConcurrentHashMap` provides:

``` java
User user = cache.computeIfAbsent(
        userId,
        id -> loadFromDatabase(id)
);
```

This is useful when the value needs to be computed if absent.

Example:

``` java
ConcurrentHashMap<Integer, String> cache =
        new ConcurrentHashMap<>();

String value = cache.computeIfAbsent(
        1,
        key -> loadFromDatabase(key)
);
```

The mapping function should generally be short and simple; do not treat
this as a general-purpose distributed lock.

------------------------------------------------------------------------

# 14. Other Important ConcurrentHashMap Methods

## `get`

``` java
V value = map.get(key);
```

Retrieves a value.

------------------------------------------------------------------------

## `put`

``` java
map.put(key, value);
```

Adds or replaces a mapping.

------------------------------------------------------------------------

## `putIfAbsent`

``` java
map.putIfAbsent(key, value);
```

Adds only when absent.

------------------------------------------------------------------------

## `remove`

``` java
map.remove(key);
```

Removes by key.

------------------------------------------------------------------------

## Conditional remove

``` java
map.remove(key, value);
```

Removes only if the current value matches.

------------------------------------------------------------------------

## `replace`

``` java
map.replace(key, newValue);
```

Replaces only if the key exists.

------------------------------------------------------------------------

## Conditional replace

``` java
map.replace(key, oldValue, newValue);
```

Replaces only if the current value matches `oldValue`.

------------------------------------------------------------------------

## `computeIfAbsent`

``` java
map.computeIfAbsent(
    key,
    k -> createValue(k)
);
```

Computes a value when absent.

------------------------------------------------------------------------

## `computeIfPresent`

``` java
map.computeIfPresent(
    key,
    (k, v) -> v + 1
);
```

Computes a replacement when present.

------------------------------------------------------------------------

## `compute`

``` java
map.compute(
    key,
    (k, v) -> newValue
);
```

Computes a mapping regardless of whether the key currently exists.

------------------------------------------------------------------------

## `merge`

Excellent for counters and aggregation:

``` java
map.merge(
    word,
    1,
    Integer::sum
);
```

If:

``` text
java -> 2
```

then:

``` java
merge("java", 1, Integer::sum)
```

produces:

``` text
java -> 3
```

This avoids the unsafe:

``` java
map.put(word, map.get(word) + 1);
```

------------------------------------------------------------------------

# 15. Frequency Counter Example

A concurrent word counter:

``` java
ConcurrentHashMap<String, Integer> counts =
        new ConcurrentHashMap<>();

counts.merge("java", 1, Integer::sum);
counts.merge("spring", 1, Integer::sum);
counts.merge("java", 1, Integer::sum);

System.out.println(counts);
```

Logical result:

``` text
java   -> 2
spring -> 1
```

The important idea:

``` text
get
 +
modify
 +
put
```

has been replaced by an atomic map operation:

``` java
merge(...)
```

------------------------------------------------------------------------

# 16. LongAdder

`AtomicInteger` / `AtomicLong` maintain one atomic value.

Under heavy contention, many threads repeatedly updating the same atomic
variable can contend on that single memory location.

`LongAdder` is designed for high-throughput counters.

Conceptually:

``` text
LongAdder

Cell 0 -> 25
Cell 1 -> 31
Cell 2 -> 44
             |
             v
           sum()
             |
             v
            100
```

Instead of forcing every update through one counter, updates can be
distributed across internal cells.

Example:

``` java
LongAdder counter = new LongAdder();

counter.increment();
counter.increment();

System.out.println(counter.sum());
```

------------------------------------------------------------------------

# 17. AtomicLong vs LongAdder

  ------------------------------------------------------------------------------
                          AtomicLong              LongAdder
  ----------------------- ----------------------- ------------------------------
  Main purpose            Atomic numeric state    High-throughput counter

  Internal model          Single atomic value     Multiple cells + base

  High contention         More contention         Designed to reduce contention
                          possible                

  Read                    `get()`                 `sum()`

  Best use                Atomic state            Metrics/statistics/frequency
                          transitions             counters
  ------------------------------------------------------------------------------

`LongAdder` is not a universal replacement for `AtomicLong`.

For example, a request counter is a good use case:

``` java
LongAdder requestCount = new LongAdder();

requestCount.increment();

long total = requestCount.sum();
```

A bank balance or state-machine variable is a different problem and may
require stronger atomic-state semantics.

------------------------------------------------------------------------

# 18. ConcurrentHashMap + LongAdder

A classic high-throughput frequency-map pattern is:

``` java
ConcurrentHashMap<String, LongAdder> counts =
        new ConcurrentHashMap<>();

counts
    .computeIfAbsent(word, key -> new LongAdder())
    .increment();
```

Conceptually:

``` text
ConcurrentHashMap
       |
       +-- "java"   -> LongAdder
       |
       +-- "spring" -> LongAdder
       |
       +-- "aws"    -> LongAdder
```

There are two levels of concurrency:

``` text
ConcurrentHashMap
        |
        v
find the key
        |
        v
LongAdder
        |
        v
distribute counter updates
```

------------------------------------------------------------------------

# 19. `LongAdder.sum()` Is Not an Atomic Snapshot

`LongAdder` is optimized for statistics.

Its `sum()` result should not be treated as an exact synchronization
point while concurrent updates are occurring.

Good examples:

-   request counters
-   metrics
-   statistics
-   frequency counting

Not a good abstraction for:

-   bank balances
-   inventory correctness
-   state-machine transitions
-   operations where an exact atomic snapshot is required

------------------------------------------------------------------------

# 20. ConcurrentHashMap Internals --- Mental Model

Do not start by trying to memorize the JDK source.

First understand the simplified structure.

``` text
ConcurrentHashMap
       |
       v
    table[]
       |
       +---- bin 0
       |
       +---- bin 1
       |
       +---- bin 2
       |
       +---- ...
```

A bin may contain:

``` text
Node -> Node -> Node
```

or, under heavy collisions, tree-based structures.

------------------------------------------------------------------------

# 21. CAS and Empty-Bin Insertion

A useful simplified model:

``` text
table[index] == null
```

Two threads want to insert.

Conceptually:

``` text
Thread A:
CAS(table[index], null, nodeA)
    |
  SUCCESS

Thread B:
CAS(table[index], null, nodeB)
    |
  FAILURE
```

CAS means:

> Replace the value only if it is still equal to the expected value.

This allows an uncontended empty-bin insertion to avoid taking a large
global lock.

------------------------------------------------------------------------

# 22. ConcurrentHashMap Does Use `synchronized`

A common misconception:

> "ConcurrentHashMap doesn't use synchronized."

It does use synchronization in parts of its implementation.

The important distinction is that it does not simply do:

``` java
synchronized (entireMap) {
    // every operation
}
```

Modern implementations combine mechanisms including:

``` text
volatile state
+
CAS
+
synchronized coordination for contended bins
+
careful resize coordination
```

The goal is to avoid unnecessary global contention.

------------------------------------------------------------------------

# 23. Java 5--7 vs Java 8+

Historical context is useful for understanding the concepts, but does
not need to be memorized.

### Java 5--7

The implementation used segments.

Conceptually:

``` text
ConcurrentHashMap
       |
  +----+----+
  |    |    |
 Seg0 Seg1 Seg2
  |    |    |
 lock lock lock
```

Different segments could be operated on concurrently.

### Java 8+

The architecture changed substantially.

The core mental model became:

``` text
ConcurrentHashMap
       |
       v
    table[]
       |
       +-- bins
             |
             +-- CAS for suitable operations
             |
             +-- synchronization for contended structures
             |
             +-- tree bins
             |
             +-- cooperative resize machinery
```

For our learning, **Java 21 is the implementation to focus on**.

The Java 8 redesign remains the foundation of the modern implementation,
including Java 21 and later JDKs.

------------------------------------------------------------------------

# 24. Java 21 Source --- What to Recognize

When looking at the Java 21 `ConcurrentHashMap` source, you will
encounter fields/classes such as:

``` text
table
nextTable
sizeCtl
transferIndex
baseCount
counterCells
Node
TreeNode
TreeBin
ForwardingNode
ReservationNode
```

Do not try to understand all of them simultaneously.

A better learning path is:

``` text
HashMap
  |
  +-- table
  +-- bucket
  +-- Node
  +-- hash
  +-- collision
  +-- resize

        ↓

ConcurrentHashMap
  |
  +-- same basic hash-table mental model
  +-- concurrent reads
  +-- CAS
  +-- bin-level coordination
  +-- tree bins
  +-- concurrent resize
  +-- distributed counting
```

------------------------------------------------------------------------

# 25. `ConcurrentHashMap` Does Not Make Arbitrary Compound Logic Atomic

This is one of the most important points.

This:

``` java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

is still a compound operation.

Using `ConcurrentHashMap` does not automatically make the whole sequence
atomic.

Prefer an operation designed for the required state transition:

``` java
map.putIfAbsent(key, value);
```

or:

``` java
map.computeIfAbsent(key, k -> createValue(k));
```

or:

``` java
map.merge(key, 1, Integer::sum);
```

depending on the problem.

------------------------------------------------------------------------

# 26. Quick Decision Guide

### Need a normal map?

``` java
HashMap
```

Use when there is no concurrent mutation requirement.

### Need a synchronized map and concurrency is simple?

``` java
Collections.synchronizedMap(...)
```

Useful when you specifically want a synchronized wrapper and its
concurrency model is acceptable.

### Need a map shared by many threads?

``` java
ConcurrentHashMap
```

Especially when there are many concurrent reads/updates.

### Need a single atomic numeric state?

``` java
AtomicInteger
AtomicLong
```

### Need a high-throughput counter?

``` java
LongAdder
```

### Need a concurrent frequency map?

``` java
ConcurrentHashMap<K, LongAdder>
```

------------------------------------------------------------------------

# 27. Key Takeaways

1.  `HashMap` is not thread-safe.
2.  Concurrent structural modification of `HashMap` is unsupported.
3.  `Collections.synchronizedMap()` provides thread-safe individual map
    operations through synchronization.
4.  A sequence of individually thread-safe operations is not necessarily
    atomic.
5.  `ConcurrentHashMap` is designed specifically for concurrent access.
6.  `putIfAbsent()` solves the common check-then-act pattern.
7.  `computeIfAbsent()` is useful for concurrent lazy initialization.
8.  `merge()` is useful for concurrent aggregation/counters.
9.  `ConcurrentHashMap` does not permit null keys or null values.
10. Modern `ConcurrentHashMap` uses a combination of CAS, volatile
    state, synchronization, and carefully designed algorithms.
11. Java 5--7 used segment-based concurrency.
12. Java 8 introduced the major architectural redesign.
13. Java 21 retains the post-Java-8 architecture.
14. `LongAdder` is designed for highly contended counters, not arbitrary
    atomic state.
15. `ConcurrentHashMap<K, LongAdder>` is a powerful pattern for
    high-throughput frequency counting.
16. Learn the concepts before trying to read the entire JDK
    implementation.

------------------------------------------------------------------------

# 28. Interview Questions --- Sourced From Real Interview Websites

> These are **not generated questions**. They are questions/topics
> published on interview-preparation sites or reported in actual
> interview experiences. Wording is preserved or lightly shortened where
> necessary. Always check the linked source for the original context.

## ConcurrentHashMap / Collections

### 1. What is ConcurrentHashMap in Java?

Source: JavaRevisited --- *Top 11 Java ConcurrentHashMap Interview
Questions with Answers*.

### 2. Is ConcurrentHashMap thread-safe?

Source: JavaRevisited --- same interview-question collection.

### 3. How does ConcurrentHashMap achieve thread safety?

Source: JavaRevisited --- same interview-question collection.

### 4. Can multiple threads read from ConcurrentHashMap at the same time?

Source: JavaRevisited --- same interview-question collection.

### 5. Can one thread read while another writes to ConcurrentHashMap?

Source: JavaRevisited --- same interview-question collection.

### 6. How does ConcurrentHashMap work internally?

Source: JavaRevisited --- same interview-question collection.

### 7. What is the difference between HashMap and ConcurrentHashMap?

Source: InterviewBit --- *Top Java Multithreading Interview Questions*.

### 8. What is ConcurrentHashMap and Hashtable? Why is ConcurrentHashMap considered faster than Hashtable?

Source: InterviewBit --- *Top Java Multithreading Interview Questions*.

### 9. What happens when you use HashMap in a multithreaded Java application?

Source: GeeksforGeeks --- *Java Collections Interview Questions and
Answers*.

### 10. What happens if two different keys of HashMap return the same `hashCode()`?

Source: GeeksforGeeks --- *Java Collections Interview Questions and
Answers*.

### 11. What is the difference between HashMap and ConcurrentHashMap?

Source: InterviewKickstart --- *60+ Java Interview Questions and
Answers*.

### 12. How does HashMap work internally, and what happens during resizing?

Source: InterviewKickstart --- *Ace Java Interview Questions for
Software Developers*.

### 13. What is the difference between ConcurrentHashMap and synchronized HashMap?

Source: InterviewKickstart --- *Ace Java Interview Questions for
Software Developers*.

------------------------------------------------------------------------

## Questions Reported in an Actual Interview Experience

GeeksforGeeks published a Paytm Software Engineer interview experience
in which the candidate reported being asked:

### 14. Questions on multithreading in Java

Source: GeeksforGeeks --- *Paytm Interview Experience for Software
Engineer \| 2+ Years Experienced*.

### 15. Questions on Collections --- HashMap, ConcurrentHashMap

Source: GeeksforGeeks --- same Paytm interview experience.

### 16. How is ConcurrentHashMap different from HashMap? Tell me the implementation of ConcurrentHashMap.

Source: GeeksforGeeks --- same Paytm interview experience.

### 17. How does HashMap work?

Source: GeeksforGeeks --- same Paytm interview experience, in the
broader Collections discussion.

------------------------------------------------------------------------

## Additional Real Interview Topics

An AllState interview experience published by GeeksforGeeks reported
questions including:

### 18. What happens when we iterate over ConcurrentHashMap and HashMap?

### 19. Difference between ConcurrentHashMap and HashMap?

### 20. Why use ConcurrentHashMap?

### 21. What is fail-fast and fail-safe?

Source: GeeksforGeeks --- *AllState Interview Questions*.

------------------------------------------------------------------------

# 29. Interview Sources

-   Baeldung --- Java Collections Interview Questions
-   Baeldung --- Java Concurrency Interview Questions
-   InterviewBit --- Top Java Multithreading Interview Questions
-   JavaRevisited --- Top Java ConcurrentHashMap Interview Questions
-   JavaRevisited --- HashMap Interview Questions
-   GeeksforGeeks --- Java Collections Interview Questions
-   GeeksforGeeks --- Paytm Software Engineer Interview Experience
-   GeeksforGeeks --- AllState Interview Questions
-   InterviewKickstart --- Java Interview Questions
