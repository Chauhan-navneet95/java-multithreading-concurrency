package com.example.concurrency.basics;

public final class ThreadBasics {
    private ThreadBasics() {
    }

    public static Thread namedWorker(String name, Runnable task) {
        return new Thread(task, name);
    }
}
