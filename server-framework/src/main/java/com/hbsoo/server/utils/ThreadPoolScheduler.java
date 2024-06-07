package com.hbsoo.server.utils;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zun.wei on 2024/6/7.
 */
public class ThreadPoolScheduler {

    private final ExecutorService[] executors;
    //private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    public ThreadPoolScheduler(String poolName, int poolSize) {
        executors = new ExecutorService[poolSize];
        for (int i = 0; i < poolSize; i++) {
            int index = i;
            executors[i] = Executors.newSingleThreadExecutor(r -> new Thread(r, poolName + " #" + index));
        }
    }

    private void execute(int hashCode, Runnable command) {
        int index = Math.abs(hashCode % executors.length);
        executors[index].execute(command);
    }

    public void execute(Object hashObj, Runnable command) {
        if (hashObj == null) {
            execute(command);
            return;
        }
        execute(hashObj.hashCode(), command);
    }

    public void execute(Runnable command) {
        Random random = new Random();
        execute(random.nextInt(), command);
    }

    public void shutdown() {
        for (ExecutorService executor : executors) {
            executor.shutdown();
        }
    }

    /*public static void main(String[] args) {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int hashCode = random.nextInt();
            System.out.println(hashCode);
        }
    }*/
}
