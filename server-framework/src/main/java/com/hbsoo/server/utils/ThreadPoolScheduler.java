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

    /**
     * 构造方法，根据给定的线程池名称和线程池大小创建一个ThreadPoolScheduler实例。
     *
     * @param poolName 线程池名称。
     * @param poolSize 线程池大小。
     */
    public ThreadPoolScheduler(String poolName, int poolSize) {
        executors = new ExecutorService[poolSize];
        for (int i = 0; i < poolSize; i++) {
            int index = i;
            executors[i] = Executors.newSingleThreadExecutor(r -> new Thread(r, poolName + " #" + index));
        }
    }

    /**
     * 根据hashCode将Runnable命令分发给对应的Executor执行。
     * 分发策略是基于hashCode的取模结果，确保命令能够被均匀地分配给不同的Executor。
     *
     * @param hashCode 命令的hashCode，用于计算分发的索引。
     * @param command 要执行的命令，实现了Runnable接口。
     */
    private void execute(int hashCode, Runnable command) {
        int index = Math.abs(hashCode % executors.length);
        executors[index].execute(command);
    }

    /**
     * 执行给定的Runnable命令，可以基于一个对象的哈希码或直接基于一个整数哈希码。
     * 这个方法提供了一种灵活的方式来进行条件执行或者基于哈希码的分派执行。
     *
     * @param hashObj 如果不为null，则使用此对象的哈希码来执行命令；如果为null，则直接执行命令。
     * @param command 要执行的Runnable命令，不包含任何参数，仅包含执行逻辑。
     */
    public void execute(Object hashObj, Runnable command) {
        if (hashObj == null) {
            execute(command);
            return;
        }
        execute(hashObj.hashCode(), command);
    }

    /**
     * 执行给定的Runnable任务。
     * 通过生成一个随机数来模拟某种形式的任务调度或延迟执行。具体的随机数用途由调用者决定。
     *
     * @param command 要执行的Runnable任务。
     */
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
