package com.example.concurrency.atomics;

public class BasicCounter implements Counter {
    private int count;

    public void increment() {
        count++;
    }

    public int getValue() {
        return count;
    }
}
