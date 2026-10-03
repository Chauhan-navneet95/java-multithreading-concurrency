package com.example.ExecutorApp;

import java.util.concurrent.*;

public class FutureDemo {

    public static void main(String[] args)
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        Future<Integer> future =
                executor.submit(() -> {

                    System.out.println(
                            "Calculating..."
                    );

                    Thread.sleep(5000);
                    
                    return 42;
                });

        System.out.println("Task submitted");

        Integer result = future.get();

        System.out.println("Result = " + result);

        executor.shutdown();
    }
}