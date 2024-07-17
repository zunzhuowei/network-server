package com.hbsoo.room.globe;

import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by zun.wei on 2024/7/7.
 */
public class GameRoomManager {

    public static final Map<String, GameRoom> CHAT_ROOM_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    public static GameRoom getGameRoom(String roomName, Function<String, GameRoom> function) {
        return CHAT_ROOM_MAP.computeIfAbsent(roomName, function);
    }

    public static GameRoom getGameRoom(String roomName) {
        return CHAT_ROOM_MAP.get(roomName);
    }

    public static void quitGameRoom(Long userId) {
        for (GameRoom gameRoom : CHAT_ROOM_MAP.values()) {
            Seat[] seats = gameRoom.getSeats();
            for (Seat seat : seats) {
                if (seat == null) {
                    continue;
                }
                if (Objects.nonNull(seat.userSession) && seat.userSession.getId().equals(userId)) {
                    seat = null;
                }
            }
        }
    }

    public static List<GameRoom> findGameRoomByUserId(Long userId) {
        return CHAT_ROOM_MAP.values().parallelStream()
                .filter(chatRoom ->
                        Arrays.stream(chatRoom.getSeats())
                                .anyMatch(seat -> {
                                    if (Objects.nonNull(seat) && Objects.nonNull(seat.userSession)) {
                                        return seat.userSession.getId().equals(userId);
                                    }
                                    return false;
                                })
                ).collect(Collectors.toList());
    }
}
