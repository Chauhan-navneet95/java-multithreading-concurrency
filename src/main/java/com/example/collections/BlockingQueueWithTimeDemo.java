package com.example.collections;

import java.time.LocalTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueWithTimeDemo {

    public static void main(String[] args)
            throws InterruptedException {

        BlockingQueue<Integer> queue =
                new ArrayBlockingQueue<>(3);

        Thread producer = new Thread(() -> {

            try {
                for (int i = 1; i <= 6; i++) {

                    System.out.println(
                        LocalTime.now()
                        + " Producer trying: " + i
                    );

                    queue.put(i);

                    System.out.println(
                        LocalTime.now()
                        + " Producer completed: " + i
                    );
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {

            try {
                for (int i = 1; i <= 6; i++) {

                    Thread.sleep(3000);

                    int value = queue.take();

                    System.out.println(
                        LocalTime.now()
                        + " Consumer took: " + value
                    );
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }
}