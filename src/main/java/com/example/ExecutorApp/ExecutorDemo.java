package com.example.ExecutorApp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorDemo {

    public static void main(String[] args) {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            System.out.println(
                    "Task 1 -> " +
                    Thread.currentThread().getName()
            );
        });

        executor.submit(() -> {
            System.out.println(
                    "Task 2 -> " +
                    Thread.currentThread().getName()
            );
        });

        executor.submit(() -> {
            System.out.println(
                    "Task 3 -> " +
                    Thread.currentThread().getName()
            );
        });

        executor.submit(() -> {
            System.out.println(
                    "Task 4 -> " +
                    Thread.currentThread().getName()
            );
        });

        executor.shutdown();
    }
}