package com.example.ExecutorApp;

import java.util.concurrent.Executor;

public class SimpleExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
        new Thread(command,"customThread").start();
    }

    public static void main(String[] args) {

        Executor executor = new SimpleExecutor();

        executor.execute(() ->
                System.out.println(
                        "Running on: " +
                        Thread.currentThread().getName()
                )
        );
    }
}