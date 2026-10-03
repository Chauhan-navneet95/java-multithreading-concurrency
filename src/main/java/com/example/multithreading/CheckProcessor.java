package com.example.multithreading;

public class CheckProcessor {
 public static void main(String[] args) {
    Runtime rt= Runtime.getRuntime();
    int cpu = rt.availableProcessors();
    System.out.println("No.s of Cpu is " + cpu);
 }
}
