package com.example.concurrency.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExecutorBasics {
    private ExecutorBasics() {
    }

    public static void runTask(Runnable task) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(task);
        }
    }
}
