package com.example.concurrency;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class LongAdderDemo {

    static final int THREADS = 10;
    static final int ITERATIONS = 1_000_000;

    public static void main(String[] args)
            throws InterruptedException {

        AtomicLong atomicCounter = new AtomicLong();
        LongAdder longAdder = new LongAdder();

        Thread[] threads = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {

            threads[i] = new Thread(() -> {

                for (int j = 0; j < ITERATIONS; j++) {
                    atomicCounter.incrementAndGet();
                    longAdder.increment();
                }

            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println(
                "AtomicLong = " + atomicCounter.get());

        System.out.println(
                "LongAdder  = " + longAdder.sum());
    }
}