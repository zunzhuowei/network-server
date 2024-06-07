package com.hbsoo.server.pool;

/**
 * Created by zun.wei on 2024/6/7.
 */

public interface HashableTask extends Runnable {
    String getKey();
}
