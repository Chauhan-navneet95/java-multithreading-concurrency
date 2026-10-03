package com.example.collections;

import java.util.concurrent.ConcurrentHashMap;

public class PutIfAbsentDemo {

    public static void main(String[] args)
            throws InterruptedException {

        ConcurrentHashMap<String, String> map =
                new ConcurrentHashMap<>();

        Runnable task = () -> {

            String thread = Thread.currentThread().getName();

            String existing =
                    map.putIfAbsent("user", thread);

            if (existing == null) {
                System.out.println(
                        thread + " successfully inserted");
            } else {
                System.out.println(
                        thread +
                        " failed. Existing value = " +
                        existing);
            }
        };

        Thread t1 = new Thread(task, "Thread-A");
        Thread t2 = new Thread(task, "Thread-B");
        Thread t3 = new Thread(task, "Thread-C");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println("\nFinal map: " + map);
    }
}