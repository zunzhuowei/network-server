package com.hbsoo.database.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zun.wei on 2024/6/15.
 */
public class RealDruidSources {

    private static final Map<String, DruidSource> map = new ConcurrentHashMap<>();

    public static DruidSource get(String sourceName) {
        return map.get(sourceName);
    }

    public static void put(String sourceName, DruidSource druidSource) {
        map.put(sourceName, druidSource);
    }

    public static boolean exist(String sourceName) {
        return map.containsKey(sourceName);
    }

    public static Map<String, DruidSource> getMap() {
        return map;
    }

}
