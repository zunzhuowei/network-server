package com.hbsoo.server.pool;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HashThreadPoolScheduler {
    private final ExecutorService[] executors;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    public HashThreadPoolScheduler(String poolName, int poolSize) {
        executors = new ExecutorService[poolSize];
        for (int i = 0; i < poolSize; i++) {
            int index = i;
            executors[i] = Executors.newSingleThreadExecutor(r -> new Thread(r, poolName + " #" + index));
        }
    }

    public void execute(HashableTask task) {
        int hash = task.getKey().hashCode();
        int index = Math.abs(hash % executors.length);
        executors[index].execute(task);
    }

    public void shutdown() {
        for (ExecutorService executor : executors) {
            executor.shutdown();
        }
    }
}

