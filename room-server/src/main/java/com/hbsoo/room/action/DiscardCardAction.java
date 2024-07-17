package com.hbsoo.room.action;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.hbsoo.permisson.PermissionAuth;
import com.hbsoo.room.ChatRoomManager;
import com.hbsoo.room.entity.Card;
import com.hbsoo.room.entity.CardType;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;
import com.hbsoo.server.annotation.OutsideMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户出牌消息
 * Created by zun.wei on 2024/6/15.
 */
@PermissionAuth
@OutsideMessageHandler(1001)
public class DiscardCardAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DiscardCardAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        String cardsJson = decoder.readStr();
        List<GameRoom> gameRooms = ChatRoomManager.findGameRoomByUserId(userId);
        if (Objects.isNull(gameRooms) || gameRooms.isEmpty()) {
            forward2AllInsideServerByTypeUseSender(decoder.toBuilder(), "room");
            return;
        }
        GameRoom gameRoom = gameRooms.get(0);
        //轮到谁出牌
        int turnNo = gameRoom.getTurnNo();
        //座位
        Seat[] seats = gameRoom.getSeats();
        //当前出的牌
        List<Card> nowCards = gameRoom.getNowCard();
        //先判断是否轮到他出牌
        if (!Objects.equals(seats[turnNo].userSession.getId(), userId)) {
            logger.error("当前不是{}出牌的回合", userId);
            return;
        }
        //想出的牌
        List<Card> discardCards = JSON.parseArray(cardsJson, Card.class);
        // 判断玩家是否有这些牌
        if (!seats[turnNo].cardsInHand.containsAll(discardCards)) {
            logger.error("玩家没有这些牌");
            return;
        }
        //判断牌型是否合法
        final CardType cardType = getCardType(discardCards);
        if (!cardType.equals(CardType.unknown)) {
            logger.error("牌型不合法");
            return;
        }
        //判断是否要得起
        if (Objects.nonNull(nowCards)) {
            int compareResult = compareCards(discardCards, nowCards);
            if (compareResult < 1) {
                logger.debug("要不起:{},{}", discardCards, nowCards);
                return;
            }
        }
        seats[turnNo].cardsInHand.removeAll(discardCards);
        seats[turnNo].cardsOutHand.addAll(discardCards);
        gameRoom.setTurnNo(turnNo == 0 ? 1 : turnNo == 1 ? 2 : 0);
        gameRoom.setNowCard(discardCards);
        //发送出牌消息
        for (Seat seat : seats) {
            if (Objects.isNull(seat) || Objects.isNull(seat.userSession)) {
                continue;
            }
            //出牌
            NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                    .msgType(1001)
                    .writeStr(JSON.toJSONString(discardCards))//出的牌
                    .writeInt(gameRoom.getTurnNo())//接下来轮到谁出牌
                    .writeInt(seats[turnNo].cardsInHand.size())//出牌人手上还有多少牌
                    ;
            outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, builder, seat.userSession.getId());
        }
        //判断是否为最后一张牌
        if (seats[turnNo].cardsInHand.size() == 0) {
            //TODO 赢牌消息1002
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        UserSession userSession = decoder.readExtendBody().getUserSession();
        Long userId = userSession.getId();
        List<GameRoom> gameRooms = ChatRoomManager.findGameRoomByUserId(userId);
        if (Objects.nonNull(gameRooms) && !gameRooms.isEmpty()) {
            return gameRooms.get(0).getRoomId();
        }
        return userId;
    }

    /**
     * 是否为王炸
     * @param cards
     * @return
     */
    private boolean isKingBoom(List<Card> cards) {
        if (cards.size() == 2) {
            if (cards.get(0).cardPoint == 16 && cards.get(1).cardPoint == 17) {
                return true;
            }
            if (cards.get(0).cardPoint == 17 && cards.get(1).cardPoint == 16) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为对子
     * @param cards
     * @return
     */
    private boolean isPair(List<Card> cards) {
        if (cards.size() == 2) {
            if (cards.get(0).cardPoint == cards.get(1).cardPoint) {
                if (isKingBoom(cards)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为三张
     * @param cards
     * @return
     */
    private boolean isTriple(List<Card> cards) {
        if (cards.size() == 3) {
            if (cards.get(0).cardPoint == cards.get(1).cardPoint
                    && cards.get(1).cardPoint == cards.get(2).cardPoint) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为普通炸弹
     * @param cards
     * @return
     */
    public boolean isCommonBoom(List<Card> cards) {
        if (cards.size() == 4) {
            //炸弹
            if (cards.get(0).cardPoint == cards.get(1).cardPoint
                    && cards.get(1).cardPoint == cards.get(2).cardPoint
                    && cards.get(2).cardPoint == cards.get(3).cardPoint) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为三带一
     * @param cards
     * @return
     */
    public boolean isTripleWithOne(List<Card> cards) {
        if (cards.size() == 4) {
            //三带一
            int count = (int) cards.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().size() == 3)
                    .count();
            if (count == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为四带二
     * @param cards
     * @return
     */
    public boolean isFourWithTwo(List<Card> cards) {
        if (cards.size() == 6) {
            int count = (int) cards.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().size() == 4)
                    .count();
            if (count == 1) {
                return true;
            }
        }
        return false;
    }
    /**
     * 判断牌型
     * @param cards
     * @return
     */
    private CardType getCardType(List<Card> cards) {
        if (cards.size() == 1) {
            return CardType.Single;
        }
        //判断是否为王炸
        if (isKingBoom(cards)) {
            return CardType.Bomb;
        }
        //对子
        if (isPair(cards)) {
            return CardType.Pair;
        }
        //三张
        if (isTriple(cards)) {
            return CardType.Triple;
        }
        //普通炸弹
        if (isCommonBoom(cards)) {
            return CardType.Bomb;
        }
        // 三带一
        if (isTripleWithOne(cards)) {
            return CardType.TripleWithOne;
        }
        //四带二
        if (isFourWithTwo(cards)) {
            return CardType.FourWithTwo;
        }
        //判断是否为顺子
        if (isStraight(cards)) {
            return CardType.Straight;
        }
        //判断是否为飞机
        if (isPlane(cards)) {
            return CardType.plane;
        }
        return CardType.unknown;
    }

    /**
     * 判断是否为顺子
     * @param cards
     * @return
     */
    private boolean isStraight(List<Card> cards) {
        if (cards.size() < 5) {
            return false;
        }
        long count = cards.stream().map(card -> card.cardPoint).distinct().count();
        //说明存在重复
        if (count != cards.size()) {
            return false;
        }
        //包含大小王的都不是顺子
        if (cards.stream().anyMatch(card -> card.cardPoint == 16 || card.cardPoint == 17)) {
            return false;
        }
        //判断是否连续
        Integer shunZiSum = cards.stream()
                .sorted(Comparator.comparing(card -> card.cardPoint))
                .map(card -> (int) card.cardPoint)
                .reduce((cardPoint1, cardPoint2) -> cardPoint1 + cardPoint1 + 1)
                .get();
        //累加所有点数
        int sum = cards.stream().mapToInt(card -> card.cardPoint).sum();
        //如果点数相等说明连续
        return sum == shunZiSum;
    }

    /**
     * 判断是否为飞机牌型
     *飞机主体：至少两组连续三张相同的牌，例如“444 555”或“777 888 999”。
     *    带单张：可以给飞机主体的每组牌各带一张单张牌。
     *    带对子：可以给飞机主体的每组牌各带一个对子。
     *    飞机可以不带单牌或对子,但不能同时带单牌和对子。
     * @param cards
     * @return
     */
    private boolean isPlane(List<Card> cards) {
        if (cards.size() < 6) {
            return false;
        }
        cards = cards.stream()
                .sorted(Comparator.comparing(card -> card.cardPoint))
                .collect(Collectors.toList());
        //有多少组三张
        int count = (int) cards.stream()
                .collect(Collectors.groupingBy(card -> card.cardPoint))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() >= 3)
                .count();
        //TODO 还缺少是否为连续的三张的判断;333,444,555
        if (count < 2) {
            return false;
        }
        int subCardSize = cards.size() - count * 3;
        //如果是带对子，则数量应该为
        int expectPairSize = count * 2;
        //如果是带单张，则数量应该为count
        if (subCardSize != expectPairSize && subCardSize != count) {
            return false;
        }
        if (subCardSize == count) {
            return true;
        }
        boolean matchPair = cards.stream()
                .collect(Collectors.groupingBy(card -> card.cardPoint))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() == 2)
                .count() == count;
        if (matchPair) {
            return true;
        }
        return false;
    }

    /**
     * 比较牌
     * @param cards1
     * @param cards2
     * @return 如果cards1大于cards2返回1，小于cards2返回-1，等于返回0
     */
    private int compareCards(List<Card> cards1, List<Card> cards2) {
        CardType cardType1 = getCardType(cards1);
        CardType cardType2 = getCardType(cards2);
        //牌型不同时比较是否有炸弹
        if (cardType1 != cardType2) {
            if (cardType1 == CardType.Bomb) {
                return 1;
            }
            if (cardType2 == CardType.Bomb) {
                return -1;
            }
            return 0;
        }
        //牌型相同时比较
        //比较单张
        if (cardType1 == CardType.Single) {
            Card card1 = cards1.get(0);
            Card card2 = cards2.get(0);
            if (card1.cardPoint > card2.cardPoint) {
                return 1;
            } else if (card1.cardPoint < card2.cardPoint) {
                return -1;
            } else {
                return 0;
            }
        }
        if (cardType1 == CardType.Pair) {
            int card1 = cards1.stream().mapToInt(card -> card.cardPoint).sum();
            int card2 = cards2.stream().mapToInt(card -> card.cardPoint).sum();
            if (card1 > card2) {
                return 1;
            } else if (card1 < card2) {
                return -1;
            } else {
                return 0;
            }
        }
        if (cardType1 == CardType.Triple) {
            int card1 = cards1.stream().mapToInt(card -> card.cardPoint).sum();
            int card2 = cards2.stream().mapToInt(card -> card.cardPoint).sum();
            if (card1 > card2) {
                return 1;
            } else if (card1 < card2) {
                return -1;
            } else {
                return 0;
            }
        }
        if (cardType1 == CardType.TripleWithOne) {
            Map<Byte, List<Card>> cardMap1 = cards1.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint));
            Map<Byte, List<Card>> cardMap2 = cards2.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint));
            Card card1 = null;
            Card card2 = null;
            for (Byte key : cardMap1.keySet()) {
                List<Card> cards = cardMap1.get(key);
                if (cards.size() == 3) {
                    card1 = cards.get(0);
                }
            }
            for (Byte key : cardMap2.keySet()) {
                List<Card> cards = cardMap2.get(key);
                if (cards.size() == 3) {
                    card2 = cards.get(0);
                }
            }
            if (card1 == null || card2 == null) {
                throw new RuntimeException("card1 or card2 is null");
            }
            if (card1.cardPoint > card2.cardPoint) {
                return 1;
            } else if (card1.cardPoint < card2.cardPoint) {
                return -1;
            } else {
                return 0;
            }
        }
        if (cardType1 == CardType.FourWithTwo) {
            Map<Byte, List<Card>> cardMap1 = cards1.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint));
            Map<Byte, List<Card>> cardMap2 = cards2.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint));
            Card card1 = null;
            Card card2 = null;
            for (Byte key : cardMap1.keySet()) {
                List<Card> cards = cardMap1.get(key);
                if (cards.size() == 4) {
                    card1 = cards.get(0);
                }
            }
            for (Byte key : cardMap2.keySet()) {
                List<Card> cards = cardMap2.get(key);
                if (cards.size() == 4) {
                    card2 = cards.get(0);
                }
            }
            if (card1 == null || card2 == null) {
                throw new RuntimeException("card1 or card2 is null2");
            }
            if (card1.cardPoint > card2.cardPoint) {
                return 1;
            } else if (card1.cardPoint < card2.cardPoint) {
                return -1;
            } else {
                return 0;
            }
        }
        if (cardType1 == CardType.Bomb) {
            boolean kingBoom1 = isKingBoom(cards1);
            if (kingBoom1) {
                return 1;
            }
            boolean kingBoom2 = isKingBoom(cards2);
            if (kingBoom2) {
                return -1;
            }
            Card card1 = cards1.get(0);
            Card card2 = cards2.get(0);
            if (card1.cardPoint > card2.cardPoint) {
                return 1;
            } else if (card1.cardPoint < card2.cardPoint) {
                return -1;
            } else {
                return 0;
            }
        }
        if (cardType1 == CardType.Straight) {
            //张数不一样，则无法比较
            if (cards1.size() != cards2.size()) {
                return 0;
            }
            int max1 = cards1.stream().mapToInt(card -> card.cardPoint).max().getAsInt();
            int max2 = cards2.stream().mapToInt(card -> card.cardPoint).max().getAsInt();
            if (max1 > max2) {
                return 1;
            } else if (max1 < max2) {
                return -1;
            } else {
                return 0;
            }
        }
        /*
        在斗地主中，飞机牌型的比较主要基于以下几个原则：
        飞机的长度：飞机牌型的长度指的是连续的三张相同数字的牌的组数。更长的飞机比短的飞机大。
        飞机的起始点：如果飞机的长度相同，那么比较飞机的起始点，即最小的那一组三张相同数字的牌。数字越大的飞机越大。
        带牌的比较：飞机可以带单牌或对子，带牌的比较仅在飞机主体完全相同的情况下进行。带牌的比较规则遵循单牌和对子的常规比较规则。
         */
        if (cardType1 == CardType.plane) {
            long plane1Count = cards1.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().size() == 3)
                    .count();
            long plane2Count = cards2.stream()
                    .collect(Collectors.groupingBy(card -> card.cardPoint))
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().size() == 3)
                    .count();
            if (plane1Count > plane2Count) {
                return 1;
            } else if (plane1Count < plane2Count) {
                return -1;
            } else {
                Optional<Card> firstCard1 = cards1.stream()
                        .collect(Collectors.groupingBy(card -> card.cardPoint))
                        .values()
                        .stream()
                        .filter(cards -> cards.size() == 3)
                        .map(cards -> cards.get(0))
                        .min(Comparator.comparingInt(card -> card.cardPoint));
                Optional<Card> firstCard2 = cards2.stream()
                        .collect(Collectors.groupingBy(card -> card.cardPoint))
                        .values()
                        .stream()
                        .filter(cards -> cards.size() == 3)
                        .map(cards -> cards.get(0))
                        .min(Comparator.comparingInt(card -> card.cardPoint));
                if (firstCard1.isPresent() && firstCard2.isPresent()) {
                    if (firstCard1.get().cardPoint > firstCard2.get().cardPoint) {
                        return 1;
                    } else if (firstCard1.get().cardPoint < firstCard2.get().cardPoint) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    return 0;
                }
            }
        }
        return 0;
    }


//    public static void main(String[] args) {
//        DiscardCardAction discardCardAction = new DiscardCardAction();
//        List<Card> cards = new ArrayList<>();
//        cards.add(new Card(3, 1));
//        cards.add(new Card(3, 1));
//        cards.add(new Card(3, 1));
//        cards.add(new Card(3, 1));
//        cards.add(new Card(4, 1));
//        cards.add(new Card(4, 1));
//        cards.add(new Card(4, 1));
//        cards.add(new Card(5, 1));
//        cards.add(new Card(6, 1));
//        cards.add(new Card(6, 1));
//        cards.add(new Card(6, 1));
//        boolean plane = discardCardAction.isPlane(cards);
//        System.out.println("plane = " + plane);
//    }

}
