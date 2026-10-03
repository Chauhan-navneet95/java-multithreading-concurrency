package com.example.collections;

import java.util.HashMap;
import java.util.Map;

public class HashMapIteratorDemo {

    public static void main(String[] args) throws InterruptedException {

        Map<Integer, String> map = new HashMap<>();

        for (int i = 1; i <= 5; i++) {
            map.put(i, "Value-" + i);
        }

        Thread iteratorThread = new Thread(() -> {
            try {
                for (Integer key : map.keySet()) {
                    System.out.println("Reading: " + key);

                    // Give the other thread time to modify the map
                    Thread.sleep(100);

                }
            } catch (Exception e) {
                System.out.println("Iterator thread: " + e);
            }
        });

        Thread modifierThread = new Thread(() -> {
            try {
                Thread.sleep(200);

                System.out.println("Adding key 6...");
                map.put(6, "Value-6");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        iteratorThread.start();
        modifierThread.start();

        iteratorThread.join();
        modifierThread.join();
    }
}


/** 
 * foreach loop internally uses iterator
 * Explicit Iterator code example 
 * Iterator<Integer> iterator = map.keySet().iterator();

while (iterator.hasNext()) {
    Integer key = iterator.next();
    System.out.println(key);
}
 */