package com.example.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SimpleThreadPool {

    public BlockingQueue<Runnable> queue;
    public List<Thread> workers;

    SimpleThreadPool(int numberOfThread, int queueCapacity){
        
        queue= new ArrayBlockingQueue<>(queueCapacity);
        workers= new ArrayList<>();

        for(int i=0;i<numberOfThread;i++){

            Thread worker = new Thread(() -> {
                while(true){
                    try{
                        Runnable task= queue.take();
                        task.run();
                    }
                    catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            worker.start();
            workers.add(worker);
        }

    }

    public void submit(Runnable task){
        try{
            queue.put(task);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
    
}
