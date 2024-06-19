package com.hbsoo.server.message;

import java.util.function.Consumer;

/**
 * Created by zun.wei on 2024/6/19.
 */
public class DecodeField<T> {

    private Class<T> tClass;
    private Consumer<T> setter;

    public DecodeField(Class<T> tClass, Consumer<T> setter) {
        this.tClass = tClass;
        this.setter = setter;
    }

    public Class<T> gettClass() {
        return tClass;
    }

    public void settClass(Class<T> tClass) {
        this.tClass = tClass;
    }

    public Consumer<T> getSetter() {
        return setter;
    }

    public void setSetter(Consumer<T> setter) {
        this.setter = setter;
    }
}
