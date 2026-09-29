package com.example.multithreading;

public class ThreadsJoinExample {
public static void main(String[] args) {
    Runnable r= ()-> {
        for (int i=0;i<500;i++){
            System.out.println("Run by "+Thread.currentThread().getName() +" i is " +i);
        }
    };

    Thread one= new Thread(r);
    Thread two=new Thread(r);
    Thread three= new Thread(r);

    one.setName("Fred");
    two.setName("Lucy");
    three.setName("Ricky");

    one.start();

    two.start();
    try {
        //here join is called on Thread two, meaning the the thread executing this thread, i.e main thread, will stop till the time thread two completes.
        //Meaning since thread one already started , thread is already started but now main thread is blocked
        //so thread three will start only after thread two completes.

        two.join();
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    three.start();
  }
}
