package com.hbsoo.gateway.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Component
public class AsyncTransactionExample {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private TransactionStatus transactionStatus;

    @Autowired
    private DataSourceTransactionManager transactionManager;

    public void startTransaction() {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        // 事物隔离级别，开启新事务 TransactionDefinition.ISOLATION_READ_COMMITTED
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        // 事务传播行为
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        //默认事务
        TransactionStatus transaction = transactionManager.getTransaction(def);
        //transactionStatus = transactionManager.getTransaction();
        this.transactionStatus = transaction;
    }

    public void executeTasks(List<Runnable> tasks) {
        List<Future<?>> futures = new ArrayList<>();

        for (Runnable task : tasks) {
            futures.add(executorService.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    // 记录错误或设置标志
                    System.out.println("Task failed: " + e.getMessage());
                    throw e; // 重新抛出异常以便在主线程中捕获
                }
            }));
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(); // 这里会抛出执行任务时的异常
            } catch (InterruptedException | ExecutionException e) {
                // 至少有一个任务失败，记录日志或处理错误
                System.out.println("A task failed, rolling back transaction.");
                rollbackTransaction();
                return;
            }
        }

        // 所有任务成功，提交事务
        commitTransaction();
    }

    private void commitTransaction() {
        transactionManager.commit(transactionStatus);
        System.out.println("Transaction committed.");
    }

    private void rollbackTransaction() {
        transactionManager.rollback(transactionStatus);
        System.out.println("Transaction rolled back.");
    }

    // 示例使用
//    public static void main(String[] args) {
//        AsyncTransactionExample example = new AsyncTransactionExample();
//        example.startTransaction();
//
//        List<Runnable> tasks = List.of(
//                () -> { /* 任务1 */ },
//                () -> { /* 任务2 */ },
//                // 更多任务...
//        );
//
//        example.executeTasks(tasks);
//    }
}
