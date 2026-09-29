package com.example.multithreading;

public class SynchronizedExample {
	private static final int WORKER_COUNT = 2;
	private static final int INCREMENTS_PER_WORKER = 100_000;

	private final Monitor monitor = new Monitor();
	private int methodCounter;
	private int blockCounter;

	public synchronized void incrementWithSynchronizedMethod() {
		methodCounter++;
	}

	public void incrementWithSynchronizedBlock() {
		synchronized (monitor) {
			blockCounter++;
		}
	}

	public synchronized int methodCounter() {
		return methodCounter;
	}

	public int blockCounter() {
		synchronized (monitor) {
			return blockCounter;
		}
	}

	private static void runWorkers(Runnable task) throws InterruptedException {
		Thread[] workers = new Thread[WORKER_COUNT];

		for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
			workers[workerIndex] = new Thread(task, "worker-" + workerIndex);
			workers[workerIndex].start();
		}

		for (Thread worker : workers) {
			worker.join();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		SynchronizedExample example = new SynchronizedExample();

		runWorkers(() -> {
			for (int increment = 0; increment < INCREMENTS_PER_WORKER; increment++) {
				example.incrementWithSynchronizedMethod();
			}
		});
		runWorkers(() -> {
			for (int increment = 0; increment < INCREMENTS_PER_WORKER; increment++) {
				example.incrementWithSynchronizedBlock();
			}
		});

		System.out.println("Synchronized method count: " + example.methodCounter());
		System.out.println("Synchronized block count: " + example.blockCounter());
	}

	private final class Monitor {
	}
}
