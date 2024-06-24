package com.hbsoo.server.annotation;

/**
 * Created by zun.wei on 2024/6/23.
 */
public enum Permission {

    ANONYMOUS_USER(0, "匿名用户"),
    USER(1, "普通用户"),
    ADMIN(2, "管理员"),
    SUPER_ADMIN(3, "超级管理员"),
    ;

    private final int value;
    private final String desc;

    Permission(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }


}
