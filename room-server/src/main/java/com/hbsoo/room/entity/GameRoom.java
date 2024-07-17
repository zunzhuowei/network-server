package com.hbsoo.room.entity;

import com.hbsoo.server.session.UserSession;

import java.util.List;
import java.util.Set;

/**
 * Created by zun.wei on 2024/7/7.
 */
public final class GameRoom {

    private Long roomId;
    private String roomName;
    private String belongServerType;
    private int belongServerId;
    //座位
    private Seat[] seats;
    //轮到哪个座位出牌(座位下标)
    private int turnNo;
    //当前出的牌,可能是单张、对、顺子、连对、飞机、飞机带单、飞机带对、炸弹、王炸
    private List<Card> nowCards;
    //当前出的牌属于哪个座位
    private int nowCardsUserNo;
    //底牌
    private Card[] diZhuCards;
    //房间状态:0:等待中、1:游戏中、2:结束
    private int status;
    //计时器(秒)
    private int timer;


    public int getNowCardsUserNo() {
        return nowCardsUserNo;
    }

    public void setNowCardsUserNo(int nowCardsUserNo) {
        this.nowCardsUserNo = nowCardsUserNo;
    }

    public int getTimer() {
        return timer;
    }
    public void setTimer(int timer) {
        this.timer = timer;
    }
    public void decrementTimer() {
        this.timer--;
    }

    public List<Card> getNowCards() {
        return nowCards;
    }

    public void setNowCards(List<Card> nowCards) {
        this.nowCards = nowCards;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

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

    public Seat[] getSeats() {
        return seats;
    }

    public void setSeats(Seat[] seats) {
        this.seats = seats;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    public List<Card> getNowCard() {
        return nowCards;
    }

    public void setNowCard(List<Card> nowCards) {
        this.nowCards = nowCards;
    }

    public Card[] getDiZhuCards() {
        return diZhuCards;
    }

    public void setDiZhuCards(Card[] diZhuCards) {
        this.diZhuCards = diZhuCards;
    }
}
