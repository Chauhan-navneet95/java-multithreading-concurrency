package com.example.collections;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapRace2 {
    public static void main(String[] args) {
        //HashMap
        //Map<Integer, Integer> map = new HashMap<>();

        //synchronized HashMap
        Map<Integer, Integer> map = Collections.synchronizedMap(new HashMap<>());

        //ConcurrentHashMap
        //Map<Integer, Integer> map = new ConcurrentHashMap<>();
        int LIMIT = 1_000_000;

        Thread t1= new Thread(()-> {
            for( int i=0; i<LIMIT;i++){
                map.put(i, i);
            }
        });
        
        Thread t2= new Thread(()-> {
            for( int i=0; i<LIMIT;i++){
                map.put(i, i);
            }
        });

        Thread t3= new Thread(()-> {
            for( int i=0; i<LIMIT;i++){
                map.put(i, i);
            }
        });
        
        t1.start();
        t2.start();
        t3.start();


        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

        System.out.println("Expected Size: "+ LIMIT);
        System.out.println("Actual Size: "+ map.size());

        //some more experiment
        int missing = 0;
        for (int i = 0; i < LIMIT; i++) {
            if (!map.containsKey(i)) {
                missing++;
            }
        }

        System.out.println("Missing keys = " + missing);
    }

}
//Lesson : HashMap provides no thread-safety guarantee for concurrent structural modification.

/*
 * Even with the same set of Keys, this programs returns size more than 100_000 on each run because ,


****
 * HashMap keeps its entry count in a size field. Calling size() returns that
 * field; it does not traverse the buckets and count entries again.
 *
 * A concurrent put is not atomic. Two threads inserting the same key can both
 * check an empty bucket, both decide the key is new, and both write a node. One
 * write may replace the other, while both operations increment size. The bucket
 * can then contain one accessible mapping even though size was incremented twice.
 *
 * Sequentially, putting the same key again updates its mapping without
 * increasing the size. ConcurrentHashMap makes these concurrent updates safe;
 * after both threads finish, this example should have 100,000 mappings because
 * both threads insert the same 100,000 keys.
 */
