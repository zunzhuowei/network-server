package com.hbsoo.server.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class SnowflakeIdGenerator {

    // 开始时间戳，这里设置为2024-01-18 09:28:23
    private static final long EPOCH = 1705541303000L;
    // 数据中心ID位数
    private static final long DATA_CENTER_ID_BITS = 1;//5;
    // 机器ID位数
    private static final long MACHINE_ID_BITS = 9;//5;
    // 序列号位数
    private static final long SEQUENCE_BITS = 12;

    // 最大数据中心ID和机器ID
    private static final long MAX_DATA_CENTER_ID = -1L ^ (-1L << DATA_CENTER_ID_BITS);
    private static final long MAX_MACHINE_ID = -1L ^ (-1L << MACHINE_ID_BITS);

    // 各部分的位移
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATA_CENTER_ID_BITS;

    // 数据中心ID
    private final long dataCenterId;
    // 机器ID
    private final long machineId;
    // 当前序列号
    private long sequence = 0L;
    // 上一次生成ID的时间戳
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param dataCenterId 数据中心ID
     * @param serverId    机器ID
     */
    public SnowflakeIdGenerator(long dataCenterId,Integer serverId) {
        long machineId = getMachineId(serverId);
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException("数据中心ID必须在0和" + MAX_DATA_CENTER_ID + "之间");
        }
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException("机器ID必须在0和" + MAX_MACHINE_ID + "之间");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    /**
     * 生成唯一ID的方法
     *
     * @return 唯一ID
     */
    public synchronized long generateId() {
        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨。拒绝生成ID。");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & ((1L << SEQUENCE_BITS) - 1);
            if (sequence == 0) {
                // 序列号溢出，等待下一个毫秒
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // 构造ID：时间戳 | 数据中心ID | 机器ID | 序列号
        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一个毫秒的方法，防止序列号溢出
     *
     * @param currentTimestamp 当前时间戳
     * @return 下一个毫秒的时间戳
     */
    private long waitNextMillis(long currentTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= currentTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    // 获取当前机器的ID方法
    private long getMachineId(Integer serverId) {
        try {
            // 获取本地主机的InetAddress
            InetAddress localHost = InetAddress.getLocalHost();
            // 获取网络接口
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);

            // 获取硬件地址（MAC地址）
            byte[] hardwareAddress = networkInterface.getHardwareAddress();
            long machineId = 0L;

            // 将MAC地址转换为long类型的机器ID
            for (byte b : hardwareAddress) {
                machineId = (machineId << 8) | (b & 0xFF);
            }
            machineId += serverId;
            // 对机器ID进行掩码，确保不超出指定位数
            return machineId & ((1L << MACHINE_ID_BITS) - 1);
        } catch (Exception e) {
            throw new RuntimeException("获取机器ID错误：" + e.getMessage());
        }
    }

    /**
     * 示例用法
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        //MAX_DATA_CENTER_ID;
        //MAX_MACHINE_ID;

        // 创建SnowflakeIdGenerator实例，数据中心ID为1，机器ID为1
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1000);

        // 生成两个ID
        long id1 = generator.generateId();
        long id2 = generator.generateId();

        // 打印生成的ID
        System.out.println("ID 1: " + id1);
        System.out.println("ID 2: " + id2);
    }
}
