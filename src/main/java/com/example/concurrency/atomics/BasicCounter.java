package com.example.concurrency.atomics;

public class BasicCounter {
    private int count;

    public void increment() {
        count++;
    }

    public int getValue() {
        return count;
    }
}
