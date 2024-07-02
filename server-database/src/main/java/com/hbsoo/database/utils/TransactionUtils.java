package com.hbsoo.database.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Created by zun.wei on 2024/6/29.
 */
public class TransactionUtils {

    @Autowired
    private DataSourceTransactionManager transactionManager;

    //开启事务,传入隔离级别
    public TransactionStatus begin(int isolationLevel) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        // 事物隔离级别，开启新事务 TransactionDefinition.ISOLATION_READ_COMMITTED
        def.setIsolationLevel(isolationLevel);
        // 事务传播行为
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        //默认事务
        TransactionStatus transaction = transactionManager.getTransaction(def);
        //将拿到的事务返回进去，才能提交。
        return transaction;
    }

    //提交事务
    public void commit(TransactionStatus transaction) {
        //提交事务
        transactionManager.commit(transaction);
    }

    //回滚事务
    public void rollback(TransactionStatus transaction) {
        transactionManager.rollback(transaction);
    }

    public static void main(String[] args) {
        TransactionUtils transactionUtils = new TransactionUtils();
        // 手动开启事务
        TransactionStatus transaction = transactionUtils.begin(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            // 修改数据回写

            // 手动提交事务
            transactionUtils.commit(transaction);
        } catch (Exception e) {
            // 手动回滚事务
            transactionUtils.rollback(transaction);
        }
    }
}
