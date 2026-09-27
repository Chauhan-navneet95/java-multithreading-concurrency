# Java Concurrency and Multithreading

A small workspace for exploring Java threads, synchronization, and concurrency utilities.

## Structure

- `src/main/java/com/example/concurrency/basics` - Thread creation and lifecycle
- `src/main/java/com/example/concurrency/synchronization` - Locks, synchronized blocks, and coordination
- `src/main/java/com/example/concurrency/executors` - Executor services and task submission
- `src/main/java/com/example/concurrency/collections` - Concurrent collections and queues
- `src/main/java/com/example/concurrency/atomics` - Atomic variables and lock-free updates
- `src/main/java/com/example/concurrency/problems` - Race conditions, deadlocks, and starvation
- `src/test/java` - Experiments and tests for each topic

## Run the starter example

With Java 21 installed:

```powershell
javac -d out src/main/java/com/example/concurrency/Main.java
java -cp out com.example.concurrency.Main
```

The project also follows the standard Maven layout. If Maven is installed later, use:

```powershell
mvn test
```
