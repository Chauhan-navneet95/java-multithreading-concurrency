package com.example.concurrency;

import com.example.concurrency.atomics.AtomicCounter;
import com.example.concurrency.atomics.BasicCounter;
import com.example.concurrency.atomics.IncrementerThread;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Thread worker = new Thread(() -> System.out.println("Running on " + Thread.currentThread().getName()));
        // worker.start();
        // worker.join();

        BasicCounter counter = new BasicCounter();
        AtomicCounter counter2 = new AtomicCounter();
        IncrementerThread t1 = new IncrementerThread(counter);
        IncrementerThread t2 = new IncrementerThread(counter);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Basic Counter 1 is "+counter.getValue());

        IncrementerThread t3 = new IncrementerThread(counter2);
        IncrementerThread t4 = new IncrementerThread(counter2);
        t3.start();
        t4.start();
        t3.join();
        t4.join();
        System.out.println("Atomic Counter is "+ counter2.getValue());


        /** Expected Result with Above code 
         * 
         * Counter 1 is 14695
         *  Atomic Counter is 20000
         * 
         */
    }
}
