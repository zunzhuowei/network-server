package com.hbsoo.room.entity;

import com.hbsoo.server.session.UserSession;

import java.util.List;

/**
 * Created by zun.wei on 2024/7/16.
 */
public final class Seat {

    // 座位号
    public int seatNo;
    // 玩家
    public UserSession userSession;
    // 手上的牌
    public List<Card> cardsInHand;
    // 已出的牌
    public List<Card> cardsOutHand;
    // 是否参加抢地主
    public boolean isGrab;

}
