package com.hbsoo.room.entity;

import com.hbsoo.server.session.UserSession;

import java.util.List;
import java.util.Set;

/**
 * Created by zun.wei on 2024/7/7.
 */
public class ChatRoom {

    private Long roomId;
    private String roomName;
    private Set<UserSession> userSessions;
    private String belongServerType;
    private int belongServerId;
    private List<String> recentMsgList;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Set<UserSession> getUserSessions() {
        return userSessions;
    }

    public void setUserSessions(Set<UserSession> userSessions) {
        this.userSessions = userSessions;
    }

    public String getBelongServerType() {
        return belongServerType;
    }

    public void setBelongServerType(String belongServerType) {
        this.belongServerType = belongServerType;
    }

    public int getBelongServerId() {
        return belongServerId;
    }

    public void setBelongServerId(int belongServerId) {
        this.belongServerId = belongServerId;
    }

    public List<String> getRecentMsgList() {
        return recentMsgList;
    }

    public void setRecentMsgList(List<String> recentMsgList) {
        this.recentMsgList = recentMsgList;
    }

    public void addUser(UserSession userSession) {
        removeUser(userSession);
        this.userSessions.add(userSession);
    }
    public void removeUser(UserSession userSession) {
        this.userSessions.removeIf(userSession1 -> userSession1.getId().equals(userSession.getId()));
        //this.userSessions.remove(userSession);
    }
    public void addRecentMsg(String msg) {
        this.recentMsgList.add(msg);
        if (this.recentMsgList.size() > 100) {
            this.recentMsgList.remove(0);
        }
    }
}
