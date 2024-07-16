package com.hbsoo.room.entity;

/**
 * Created by zun.wei on 2024/7/16.
 */
public enum CardType {

    Single(1),//单张
    Pair(2),//对子
    Triple(3),//三张
    TripleWithOne(4),//三带一
    FourWithTwo(5),//四带二
    Bomb(6),//炸弹
    Straight(7),//顺子
    plane(8),//飞机
    unknown(9),//未知牌型
    ;

    public static CardType getCardType(int value) {
        for (CardType cardType : CardType.values()) {
            if (cardType.value == value) {
                return cardType;
            }
        }
        return null;
    }
    public final int value;
    CardType(int value) {
        this.value = value;
    }

}
