package com.example.concurrency.synchronization;

public class SynchronizedCounter {
    private int value;

    public synchronized void increment() {
        value++;
    }

    public synchronized int value() {
        return value;
    }
}
