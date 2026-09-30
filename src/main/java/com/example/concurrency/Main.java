package com.example.concurrency;

import com.example.concurrency.atomics.BasicCounter;
import com.example.concurrency.atomics.IncrementerThread;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Thread worker = new Thread(() -> System.out.println("Running on " + Thread.currentThread().getName()));
        // worker.start();
        // worker.join();

        BasicCounter counter = new BasicCounter();
        IncrementerThread t1 = new IncrementerThread(counter);
        IncrementerThread t2 = new IncrementerThread(counter);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(counter.getValue());
    }
}
