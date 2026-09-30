package com.example.concurrency.atomics;

public class IncrementerThread extends Thread {
    private BasicCounter counter;

    public IncrementerThread(BasicCounter counter) {
        this.counter = counter;

    }

    public void run() {
        for (int i = 0; i < 10000; i++) {
            counter.increment();
        }
    }

}
