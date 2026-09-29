package com.example.multithreading;

public class MultipleThreadsExample {
 public static void main(String[] args) {
    Runnable r= ()-> {
        for (int i=0;i<100;i++){
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
    three.start();
  }
}
