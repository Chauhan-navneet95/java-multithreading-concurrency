package com.example.concurrency;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> System.out.println("Running on " + Thread.currentThread().getName()));
        worker.start();
        worker.join();
    }
}
