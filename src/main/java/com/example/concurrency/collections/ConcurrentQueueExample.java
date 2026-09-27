package com.example.concurrency.collections;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class ConcurrentQueueExample {
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public void add(String item) {
        queue.offer(item);
    }

    public String poll() {
        return queue.poll();
    }
}
