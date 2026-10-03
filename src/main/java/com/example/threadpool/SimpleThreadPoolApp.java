package com.example.threadpool;

public class SimpleThreadPoolApp {

    public static void main(String[] args) {
        
        SimpleThreadPool pool= new SimpleThreadPool(3, 10);

        for (int i=0 ;i<=8 ;i++){
            int taskid=i;
            pool.submit(() -> {
                System.out.println( Thread.currentThread().getName() +" executing task" + taskid);
            });
        }
    }
}
