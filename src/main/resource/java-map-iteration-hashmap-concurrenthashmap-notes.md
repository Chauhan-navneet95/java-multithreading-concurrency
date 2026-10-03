# Java Map Traversal & Iterators — HashMap vs ConcurrentHashMap

## 1. Map traversal basics

A `Map` is **not index-based** like a `List`.

```java
for (int i = 0; i < map.size(); i++) {
    // map.get(i) uses i as a KEY, not an index
}
```

Use `entrySet()` when you need both key and value:

```java
for (Map.Entry<Integer, String> entry : map.entrySet()) {
    System.out.println(entry.getKey() + " = " + entry.getValue());
}
```

Explicit iterator:

```java
Iterator<Map.Entry<Integer, String>> iterator =
        map.entrySet().iterator();

while (iterator.hasNext()) {
    Map.Entry<Integer, String> entry = iterator.next();
    System.out.println(entry.getKey() + " = " + entry.getValue());
}
```

## 2. What does `map.keySet()` return?

`keySet()` returns a **view backed by the original map**. It does not copy the keys.

```java
Map<Integer, String> map = new HashMap<>();
map.put(10, "A");
map.put(20, "B");

Set<Integer> keys = map.keySet();

map.put(30, "C");

System.out.println(keys); // [10, 20, 30]
```

A `Set` has no positional `get(i)`:

```java
keys.get(i); // ❌
```

If an actual array is required:

```java
Integer[] keyArray = map.keySet().toArray(new Integer[0]);

for (int i = 0; i < keyArray.length; i++) {
    System.out.println(keyArray[i]);
}
```

The array is a separate copy.

## 3. HashMap iterator — fail-fast

`HashMap` iterators are **fail-fast on a best-effort basis**.

```java
Map<Integer, String> map = new HashMap<>();
map.put(1, "A");
map.put(2, "B");
map.put(3, "C");

for (Integer key : map.keySet()) {
    System.out.println(key);

    if (key == 2) {
        map.put(4, "D");   // structural modification
    }
}
```

Typical result:

```text
1
2
ConcurrentModificationException
```

Conceptually, `HashMap` maintains `modCount`. The iterator stores an `expectedModCount` when created and checks for a mismatch during traversal.

```text
iterator created
      ↓
expectedModCount = map.modCount
      ↓
map is structurally modified
      ↓
modCount changes
      ↓
iterator.next()
      ↓
mismatch → ConcurrentModificationException
```

**Important:** fail-fast is a best-effort bug-detection mechanism, not a synchronization guarantee.

## 4. Safe removal during HashMap iteration

Use the iterator's own `remove()`:

```java
Map<Integer, String> map = new HashMap<>();
map.put(1, "A");
map.put(2, "B");
map.put(3, "C");

Iterator<Integer> iterator = map.keySet().iterator();

while (iterator.hasNext()) {
    Integer key = iterator.next();

    if (key == 2) {
        iterator.remove();   // ✅
    }
}
```

Directly modifying the map during iteration:

```java
map.remove(2); // ❌ may cause ConcurrentModificationException
```

## 5. ConcurrentHashMap iterator — weakly consistent

`ConcurrentHashMap` iterators are **weakly consistent**.

```java
ConcurrentHashMap<Integer, String> map =
        new ConcurrentHashMap<>();

map.put(1, "A");
map.put(2, "B");
map.put(3, "C");

for (Integer key : map.keySet()) {
    System.out.println(key);

    if (key == 2) {
        map.put(4, "D");   // allowed
    }
}
```

No `ConcurrentModificationException` is thrown merely because the map is modified.

The iterator may see `4`, or may not, depending on timing.

Weakly consistent means:

- no fail-fast exception merely because of concurrent modification
- some concurrent modifications may be observed
- some may not be observed
- traversal remains safe
- it is **not a snapshot**

If you need a snapshot:

```java
Map<Integer, String> snapshot = new HashMap<>(concurrentMap);
```

## 6. Two-thread example

### HashMap

```java
Map<Integer, String> map = new HashMap<>();

for (int i = 1; i <= 5; i++) {
    map.put(i, "Value-" + i);
}

Thread reader = new Thread(() -> {
    try {
        for (Integer key : map.keySet()) {
            System.out.println("Reading: " + key);
            Thread.sleep(100);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

Thread writer = new Thread(() -> {
    try {
        Thread.sleep(200);
        System.out.println("Adding key 6");
        map.put(6, "Value-6");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

reader.start();
writer.start();

reader.join();
writer.join();
```

Typically, the iterator detects the structural modification and throws `ConcurrentModificationException`. Do not rely on this behavior as synchronization.

### ConcurrentHashMap

```java
ConcurrentHashMap<Integer, String> map =
        new ConcurrentHashMap<>();

for (int i = 1; i <= 5; i++) {
    map.put(i, "Value-" + i);
}

Thread reader = new Thread(() -> {
    for (Integer key : map.keySet()) {
        System.out.println("Reading: " + key);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
});

Thread writer = new Thread(() -> {
    try {
        Thread.sleep(200);
        System.out.println("Adding key 6");
        map.put(6, "Value-6");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

reader.start();
writer.start();

reader.join();
writer.join();
```

The traversal continues safely. Key `6` may or may not be observed.

## 7. `Collections.synchronizedMap()` and iteration

This is a common trap:

```java
Map<Integer, String> map =
        Collections.synchronizedMap(new HashMap<>());
```

Individual operations are synchronized, but iteration requires external synchronization:

```java
synchronized (map) {
    for (Integer key : map.keySet()) {
        System.out.println(key);
    }
}
```

The wrapper does not automatically hold its lock for your entire iteration.

## 8. Fail-fast vs "fail-safe"

You will often hear:

```text
HashMap           → fail-fast
ConcurrentHashMap → fail-safe
```

"Fail-safe" is informal terminology. The precise term for `ConcurrentHashMap` is:

> **Weakly consistent iterator**

Prefer:

```text
HashMap
→ fail-fast iterator (best effort)

ConcurrentHashMap
→ weakly consistent iterator
```

## 9. Quick comparison

| Feature | HashMap | synchronizedMap | ConcurrentHashMap |
|---|---|---|---|
| Thread-safe map operations | ❌ | ✅ | ✅ |
| Iterator | Fail-fast | Fail-fast | Weakly consistent |
| Concurrent modification during iteration | Unsafe | Requires external synchronization | Supported |
| `ConcurrentModificationException` | Possible | Possible | Not merely due to concurrent modification |
| Snapshot iterator | ❌ | ❌ | ❌ |
| `keySet()` | Backed view | Backed view | Backed view |
| Allows null | ✅ | ✅ | ❌ |

## 10. Mental model

```text
HashMap
   |
   +-- iterator
   +-- tracks expected modification count
   +-- structural modification detected
             ↓
       ConcurrentModificationException


ConcurrentHashMap
   |
   +-- iterator
   +-- designed for concurrent traversal
   +-- concurrent modifications allowed
   +-- may observe some changes
   +-- may miss some changes
   +-- no snapshot
```

## Key takeaways

1. `Map` is not index-based; use `entrySet()`, `keySet()`, or an iterator.
2. `keySet()` returns a **backed view**, not a copied array.
3. `HashMap` iterators are **fail-fast (best effort)**.
4. Modifying a `HashMap` directly during iteration can cause `ConcurrentModificationException`.
5. `iterator.remove()` is the supported way to remove the current element during iteration.
6. `ConcurrentHashMap` iterators are **weakly consistent**.
7. A `ConcurrentHashMap` iterator is **not a snapshot**.
8. `Collections.synchronizedMap()` requires external synchronization for iteration.
9. "Fail-safe" is informal; **weakly consistent** is the precise term for `ConcurrentHashMap`.
