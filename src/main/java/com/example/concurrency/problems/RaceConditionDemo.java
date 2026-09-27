package com.example.concurrency.problems;

public final class RaceConditionDemo {
    private int value;

    public void increment() {
        value++;
    }

    public int value() {
        return value;
    }
}
