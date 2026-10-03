package com.example.threadpool.forkjoinpool;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

public class ForkJoinDemoApp {

    public static void main(String[] args) {
        int[] numbers = {
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15,
                16, 17, 18, 19, 20
        };

        ForkJoinPool pool = new ForkJoinPool();
        try {
            long sum = pool.invoke(new SumTask(numbers, 0, numbers.length));
            System.out.println("Numbers: " + Arrays.toString(numbers));
            System.out.println("Sum: " + sum);
        } finally {
            pool.shutdown();
        }
    }
}
