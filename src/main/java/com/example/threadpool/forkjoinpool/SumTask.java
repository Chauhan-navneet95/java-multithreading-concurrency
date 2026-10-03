package com.example.threadpool.forkjoinpool;

import java.util.concurrent.RecursiveTask;

class SumTask extends RecursiveTask<Long> {

    private static final int THRESHOLD = 2;

    private final int[] numbers;
    private final int start;
    private final int end;

    SumTask(int[] numbers, int start, int end) {
        this.numbers = numbers;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {

        int length = end - start;

        if (length <= THRESHOLD) {

            long sum = 0;

            for (int i = start; i < end; i++) {
                sum += numbers[i];
            }

            return sum;
        }

        int mid = start + length / 2;

        SumTask left =
                new SumTask(numbers, start, mid);

        SumTask right =
                new SumTask(numbers, mid, end);

        left.fork();

        long rightResult = right.compute();

        long leftResult = left.join();

        return leftResult + rightResult;
    }
}