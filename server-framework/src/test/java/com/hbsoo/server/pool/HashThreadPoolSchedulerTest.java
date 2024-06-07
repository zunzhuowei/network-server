package com.hbsoo.server.pool;


public class HashThreadPoolSchedulerTest {
    public static void main(String[] args) {
        int poolSize = 4;
        HashThreadPoolScheduler scheduler = new HashThreadPoolScheduler("test", poolSize);

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            scheduler.execute(new HashableTask() {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    System.out.println("Task " + taskId + " executed by " + threadName);
                }

                @Override
                public String getKey() {
                    return "task" + (taskId % 3); // simulate similar keys for some tasks
                }
            });
        }

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            scheduler.execute(new HashableTask() {
                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    System.out.println("Task " + taskId + " executed by " + threadName);
                }

                @Override
                public String getKey() {
                    return "task" + (taskId % 3); // simulate similar keys for some tasks
                }
            });
        }

        scheduler.shutdown();
    }
}

