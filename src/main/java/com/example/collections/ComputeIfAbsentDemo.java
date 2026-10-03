package com.example.collections;

import java.util.concurrent.ConcurrentHashMap;

public class ComputeIfAbsentDemo {

    public static void main(String[] args)
            throws InterruptedException {

        ConcurrentHashMap<String, Integer> map =
                new ConcurrentHashMap<>();

        Runnable task = () -> {

            Integer value = map.computeIfAbsent(
                    "Java",
                    key -> {

                        System.out.println(
                                Thread.currentThread().getName()
                                + " computing value..."
                        );

                        sleep(2000);

                        return 100;
                    }
            );

            System.out.println(
                    Thread.currentThread().getName()
                    + " got " + value
            );
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");
        Thread t3 = new Thread(task, "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println(map);
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}