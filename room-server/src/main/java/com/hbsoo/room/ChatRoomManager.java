package com.hbsoo.room;

import com.hbsoo.room.entity.ChatRoom;

import java.util.Map;
import java.util.function.Function;

/**
 * Created by zun.wei on 2024/7/7.
 */
public class ChatRoomManager {

    public static final Map<String, ChatRoom> CHAT_ROOM_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    public static ChatRoom getChatRoom(String roomName, Function<String, ChatRoom> function) {
        return CHAT_ROOM_MAP.computeIfAbsent(roomName, function);
    }

    public static ChatRoom getChatRoom(String roomName) {
        return CHAT_ROOM_MAP.get(roomName);
    }

}
