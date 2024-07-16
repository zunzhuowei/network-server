package com.hbsoo.room.entity;

import java.util.*;

/**
 * Created by zun.wei on 2024/7/16.
 */
public final class Card {

    /**
     * 点数，3：3点，4：4点，5：5点，6：6点，7：7点，8：8点，9：9点，10：10点，
     * 11：J，12：Q，13：K，14：A，15：2，16：小王，17：大王
     */
    public byte cardPoint;
    /**
     * 牌排序，0:方块，1:梅花，2:红桃，3:黑桃
     */
    public byte cardSort;

    public Card(int cardPoint, int cardSort) {
        this.cardPoint = (byte) cardPoint;
        this.cardSort = (byte) cardSort;
    }


    public static List<Card> newCards() {
        //初始化一副牌
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            for (int j = 3; j <= 17; j++) {
                Card card = new Card(j, i);
                cards.add(card);
            }
            Collections.shuffle(cards);
        }
        Collections.shuffle(cards);
        return cards;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return cardPoint == card.cardPoint && cardSort == card.cardSort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardPoint, cardSort);
    }

    @Override
    public String toString() {
        return "Card{" +
                "cardPoint=" + cardPoint +
                ", cardSort=" + cardSort +
                '}';
    }
}
