# Java Thread States

Java defines six states in `Thread.State`:

| State | Meaning |
| --- | --- |
| `NEW` | Thread is created but `start()` has not been called. |
| `RUNNABLE` | Thread is eligible to run or is currently running. Java does not expose a separate `RUNNING` state. |
| `BLOCKED` | Thread is waiting to acquire a monitor lock. |
| `WAITING` | Thread waits indefinitely for another thread's action, such as after `join()` or `wait()`. |
| `TIMED_WAITING` | Thread waits for a limited time, such as during `sleep()` or timed `join()`. |
| `TERMINATED` | Thread has finished execution; often called dead. |

`Runnable` is an interface for work a thread can execute, not a thread state. A thread typically moves from `NEW` to `RUNNABLE`, may enter a waiting or blocked state, and eventually becomes `TERMINATED`.
