package com.hbsoo.room;

import com.hbsoo.room.entity.ChatRoom;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public static void quitChatRoom(Long userId) {
        for (ChatRoom chatRoom : CHAT_ROOM_MAP.values()) {
            chatRoom.getUserSessions().removeIf(userSession -> userSession.getId().equals(userId));
        }
    }

    public static List<ChatRoom> findChatRoomByUserId(Long userId) {
        return CHAT_ROOM_MAP.values().parallelStream()
                .filter(chatRoom ->
                        chatRoom.getUserSessions().stream().anyMatch
                                (userSession -> userSession.getId().equals(userId))
                ).collect(Collectors.toList());
    }
}
