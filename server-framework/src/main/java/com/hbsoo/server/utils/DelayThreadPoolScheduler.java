package com.hbsoo.server.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 延迟线程池
 * Created by zun.wei on 2024/6/8.
 */
public class DelayThreadPoolScheduler {

    private static final AtomicInteger instanceCounter = new AtomicInteger(0);
    final ScheduledExecutorService threadPool;
    private final String uniquePoolName;

    public DelayThreadPoolScheduler(String poolName, int poolSize) {
        if(poolSize <= 0) {
            throw new IllegalArgumentException("线程池大小必须大于0");
        }
        // 为避免线程名冲突，添加一个唯一的标识符
        uniquePoolName = poolName + "-" + instanceCounter.incrementAndGet();
        threadPool = Executors.newScheduledThreadPool(poolSize, r -> new Thread(r, uniquePoolName));
    }

    /**
     * 请求关闭线程池。
     * <p>
     * 此方法调用线程池的shutdown方法，以优雅地停止所有正在执行的任务。
     * 它不会立即终止线程池，而是停止接受新的任务，并等待已提交任务的完成。
     * 注意，这并不意味着线程池中的所有任务都会立即完成，而是不再接受新的任务。
     *
     * @see DelayThreadPoolScheduler#shutdown()
     */
    public void shutdown() {
        threadPool.shutdown();
    }

    /**
     * 销毁线程池，立即停止所有任务并关闭线程池。
     */
    public void destroy() {
        threadPool.shutdownNow();
        // 等待所有线程终止，可设置一个合理的超时时间
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                // 如果超时，强制中断所有线程
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            threadPool.shutdownNow();
        }
    }

    /**
     * 安排一个Runnable任务在未来某个时间点执行。这个方法用于安排一个任务在给定的延迟后仅执行一次。
     * 此方法通过利用线程池的能力，安排给定的Runnable任务在指定延迟后执行。
     * 延迟时间由参数delay和unit指定，任务执行的具体时机由线程池内部管理。
     *
     * @param command 要执行的任务，应该是一个无参数的Runnable对象。
     * @param delay 指定任务执行前的延迟时间。
     * @param unit 延迟时间的单位，例如秒、毫秒等。
     * @return 返回一个ScheduledFuture对象，该对象可用于取消任务或查询任务的执行状态。
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return threadPool.schedule(command, delay, unit);
    }

    /**
     * 定期以固定延迟执行给定的Runnable任务。任务间的间隔是基于上一个任务开始的时间
     * <p>
     * 此方法用于安排一个可重复执行的任务，从首次执行后的指定延迟开始，然后以固定的延迟重复执行。
     * 延迟和周期时间单位由TimeUnit参数指定。
     *
     * @param command 要执行的任务，必须是一个Runnable对象。
     * @param delay   首次执行之前的延迟时间。
     * @param period  两次连续执行之间的固定延迟时间。
     * @param unit    时间单位，用于指定延迟和周期时间。
     * @return 返回一个ScheduledFuture对象，该对象可用于取消任务或查询任务的执行状态。
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
        return threadPool.scheduleAtFixedRate(command, delay, period, unit);
    }

    /**
     * 使用固定延迟安排任务。任务间的间隔是基于上一个任务完成的时间
     * <p>
     * 此方法用于安排一个Runnable任务，该任务将以固定的延迟重复执行。首次执行后，后续执行将按照指定的延迟和周期进行。
     * 这是一种具有弹性调度能力的方式，适用于需要在每次执行结束后等待一段时间后才开始下一次执行的场景，如周期性任务调度。
     *
     * @param command 要执行的任务，应该是一个无参数的Runnable对象。
     * @param delay   第一次执行任务之前的延迟时间。
     * @param period  两次连续执行之间的固定延迟时间。
     * @param unit    时间单位，用于指定延迟和周期的时间度量单位。
     * @return 返回一个ScheduledFuture对象，该对象可用于取消任务或查询任务的执行状态。
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
        return threadPool.scheduleWithFixedDelay(command, delay, period, unit);
    }

}
