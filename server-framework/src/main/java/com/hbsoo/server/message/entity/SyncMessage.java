package com.hbsoo.server.message.entity;

import java.util.concurrent.CountDownLatch;

/**
 * Created by zun.wei on 2024/7/9.
 */
public final class SyncMessage {

    private HBSPackage.Decoder decoder;

    private final CountDownLatch countDownLatch;

    public SyncMessage(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public HBSPackage.Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(HBSPackage.Decoder decoder) {
        this.decoder = decoder;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

}
