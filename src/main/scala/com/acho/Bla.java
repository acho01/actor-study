package com.acho;

import java.util.Random;
import java.util.concurrent.*;

public class Bla {

    private static BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<Integer>(10);

    public static void main(String[] args) throws InterruptedException {
        Thread producer = new Thread(Bla::producer);
        Thread consumer = new Thread(Bla::consumer);

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }

    private static void producer() {
        Random r = new Random();
        while (true) {
            try {
                blockingQueue.put(r.nextInt(100));
            } catch (InterruptedException e) {

            }
        }
    }

    private static void consumer() {
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Random r = new Random();
            int n = r.nextInt(10);
            if (n == 0) {
                try {
                    int taken = blockingQueue.take();
                    System.out.println("Taken " + taken + " Q size: "+blockingQueue.size());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
