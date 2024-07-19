package com.hbsoo.room.action.inside;

import com.alibaba.fastjson.JSON;
import com.hbsoo.room.entity.Card;
import com.hbsoo.room.entity.CardType;
import com.hbsoo.room.entity.GameRoom;
import com.hbsoo.room.entity.Seat;
import com.hbsoo.room.globe.GameRoomManager;
import com.hbsoo.server.annotation.InsideServerMessageHandler;
import com.hbsoo.server.message.entity.NetworkPacket;
import com.hbsoo.server.message.server.ServerMessageDispatcher;
import com.hbsoo.server.session.OutsideUserProtocol;
import com.hbsoo.server.session.OutsideUserSessionManager;
import com.hbsoo.server.session.UserSession;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 超时自动出牌
 * Created by zun.wei on 2024/6/15.
 */
@InsideServerMessageHandler(1001)
public class AutoDiscardCardAction extends ServerMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(AutoDiscardCardAction.class);
    @Autowired
    private OutsideUserSessionManager outsideUserSessionManager;

    @Override
    public void handle(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        boolean isAuto = decoder.readBoolean();
        String roomName = decoder.readStr();
        GameRoom gameRoom = GameRoomManager.getGameRoom(roomName);
        if (gameRoom.getStatus() != 2) {
            return;
        }
        //轮到谁出牌
        int turnNo = gameRoom.getTurnNo();
        //上一把是谁出的牌
        int nowCardsUserNo = gameRoom.getNowCardsUserNo();
        long userId;
        String cardsJson;
        if (isAuto) {
            int timer = gameRoom.getTimer();
            if (timer > 0) {
                gameRoom.decrementTimer();
                redirectMessage(ctx, decoder, 1, TimeUnit.MILLISECONDS);
                logger.debug("自动出牌倒计时:{}", timer);
                //推送倒计时给客户端
                NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                        .msgType(1004)
                        .writeInt(timer);
                for (Seat seat : gameRoom.getSeats()) {
                    UserSession session = seat.userSession;
                    outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, builder, session.getId());
                }
                return;
            }
            List<Card> nowCard = gameRoom.getNowCard();
            Seat[] seats = gameRoom.getSeats();
            UserSession userSession = seats[turnNo].userSession;
            List<Card> cardsInHand = seats[turnNo].cardsInHand;
            userId = userSession.getId();
            //如果上一把的牌是自己出的
            if (Objects.isNull(nowCard) || nowCardsUserNo == turnNo) {
                Optional<Card> min = cardsInHand.stream().min(Comparator.comparingInt(c -> c.cardPoint));
                boolean present = min.isPresent();
                if (!present) {
                    logger.debug("上一把的牌是自己出的,这一把没有牌了，说明已经赢了！");
                    return;
                }
                List<Card> cards = new ArrayList<>();
                cards.add(min.get());
                cardsJson = JSON.toJSONString(cards);
            } else {
                //判断是否要不起
                List<Card> bigThanCards = getBigThanCards(cardsInHand, nowCard);
                if (Objects.isNull(bigThanCards)) {
                    logger.debug("{}:要不起:{},{}", userId, cardsInHand, nowCard);
                    gameRoom.setTurnNo(turnNo == 0 ? 1 : turnNo == 1 ? 2 : 0);
                    gameRoom.setTimer(10);
                    //自动出牌
                    NetworkPacket.Builder autoDiscardBuilder = NetworkPacket.Builder
                            .withDefaultHeader()
                            .msgType(1001)
                            .writeBoolean(true)
                            .writeStr(gameRoom.getRoomName());
                    redirectMessage(ctx, autoDiscardBuilder, 1,TimeUnit.MILLISECONDS);
                    //通知玩家当前玩家要不起
                    NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                            .msgType(1003)
                            .writeLong(userId)
                            .writeStr("要不起");
                    for (Seat seat : seats) {
                        UserSession session = seat.userSession;
                        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, builder, session.getId());
                    }
                    return;
                } else {
                    cardsJson = JSON.toJSONString(bigThanCards);
                }
            }
        } else {
            userId = decoder.readLong();
            cardsJson = decoder.readStr();
        }
        gameRoom.setTimer(10);
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
        if (cardType.equals(CardType.unknown)) {
            logger.error("牌型不合法");
            return;
        }
        //判断是否要得起
        if (Objects.nonNull(nowCards)) {
            int compareResult = compareCards(discardCards, nowCards);
            if (compareResult < 1 && turnNo != nowCardsUserNo) {
                logger.debug("要不起:{},{}", discardCards, nowCards);
                return;
            }
        }
        seats[turnNo].cardsInHand.removeAll(discardCards);
        seats[turnNo].cardsOutHand.addAll(discardCards);
        gameRoom.setTurnNo(turnNo == 0 ? 1 : turnNo == 1 ? 2 : 0);
        gameRoom.setNowCard(discardCards);
        gameRoom.setNowCardsUserNo(turnNo);
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
        //把剩下的牌发给出牌人
        List<Card> collect = seats[turnNo].cardsInHand.stream()
                .sorted(Comparator.comparingInt(card -> card.cardSort))
                .sorted(Comparator.comparingInt(card -> card.cardPoint))
                .collect(Collectors.toList());
        NetworkPacket.Builder builder = NetworkPacket.Builder.withDefaultHeader()
                .msgType(103)
                .writeStr(JSON.toJSONString(collect));
        outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, builder, userId);
        //判断是否为最后一张牌
        if (seats[turnNo].cardsInHand.size() == 0) {
            gameRoom.setStatus(3);
            //赢牌消息1002
            NetworkPacket.Builder b = NetworkPacket.Builder.withDefaultHeader()
                    .msgType(1002)
                    .writeLong(userId)
                    .writeStr(gameRoom.getRoomName());
            for (Seat seat : seats) {
                UserSession session = seat.userSession;
                outsideUserSessionManager.sendMsg2User(OutsideUserProtocol.BINARY_WEBSOCKET, b, session.getId());
            }
            //重新开始牌局
            redirectMessage(ctx, NetworkPacket.Builder
                            .withDefaultHeader()
                            .msgType(1003)
                            .writeStr(gameRoom.getRoomName()),
                    10);
            return;
        }
        if (isAuto) {
            redirectMessage(ctx, decoder, 1,TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public Object threadKey(ChannelHandlerContext ctx, NetworkPacket.Decoder decoder) {
        boolean isAuto = decoder.readBoolean();
        String roomName = decoder.readStr();
        GameRoom gameRoom = GameRoomManager.getGameRoom(roomName);
        return Objects.nonNull(gameRoom) ? gameRoom.getRoomId() : roomName;
    }

    /**
     * 是否为王炸
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     * 飞机主体：至少两组连续三张相同的牌，例如“444 555”或“777 888 999”。
     * 带单张：可以给飞机主体的每组牌各带一张单张牌。
     * 带对子：可以给飞机主体的每组牌各带一个对子。
     * 飞机可以不带单牌或对子,但不能同时带单牌和对子。
     *
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
        if (count < 2) {
            return false;
        }
        //是否为连续的三张的判断;333,444,555
        Optional<List<List<Card>>> theSameCardPointWithSize = findTheSameCardPointWithSize(cards, 3);
        if (!theSameCardPointWithSize.isPresent()) {
            return false;
        }
        List<List<Card>> lists = theSameCardPointWithSize.get();
        byte cardPoint = lists.get(0).get(0).cardPoint;
        byte cardPoint1 = lists.get(1).get(1).cardPoint;
        if (cardPoint != cardPoint1) {
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
     *
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

    /**
     * 查找比当前牌更大的牌出来
     *
     * @param cardsInHand
     * @param nowCards
     * @return
     */
    private List<Card> getBigThanCards(List<Card> cardsInHand, List<Card> nowCards) {
        if (Objects.isNull(cardsInHand) || cardsInHand.isEmpty()) {
            return null;
        }
        if (Objects.isNull(nowCards)) {
            Optional<Card> min = cardsInHand.stream().min(Comparator.comparingInt(c -> c.cardPoint));
            return Collections.singletonList(min.get());
        }
        CardType cardType = getCardType(nowCards);
        if (cardType == CardType.Single) {
            Optional<Card> min = cardsInHand.stream().filter(card -> card.cardPoint > nowCards.get(0).cardPoint)
                    .min(Comparator.comparingInt(c -> c.cardPoint));
            return min.map(Collections::singletonList).orElse(null);
        }
        if (cardType == CardType.Pair) {
            return getCardList(cardsInHand, nowCards, 2);
        }
        if (cardType == CardType.Triple) {
            return getCardList(cardsInHand, nowCards, 3);
        }
        if (cardType == CardType.TripleWithOne) {
            List<Card> cardList = getCardList(cardsInHand, nowCards, 3);
            if (Objects.isNull(cardList)) {
                return null;
            }
            //如果是炸弹直接返回
            if (cardList.size() == 4 || cardList.size() == 2) {
                return cardList;
            }
            Card card = cardList.get(0);
            Optional<Card> min = cardsInHand.stream()
                    .filter(c -> c.cardPoint != card.cardPoint)
                    .min(Comparator.comparingInt(c -> c.cardPoint));
            if (min.isPresent()) {
                cardList.add(min.get());
                return cardList;
            } else {
                return null;
            }
        }
        if (cardType == CardType.FourWithTwo) {
            List<Card> cardList = getCardList(cardsInHand, nowCards, 4);
            if (Objects.isNull(cardList)) {
                return null;
            }
            //如果是炸弹直接返回
            if (cardList.size() == 4 || cardList.size() == 2) {
                return cardList;
            }
            Card card = cardList.get(0);
            List<Card> collect = cardsInHand.stream()
                    .filter(c -> c.cardPoint != card.cardPoint)
                    .sorted(Comparator.comparingInt(c -> c.cardPoint))
                    .limit(2).collect(Collectors.toList());
            if (collect.size() == 2) {
                cardList.addAll(collect);
                return cardList;
            } else {
                return null;
            }
        }
        if (cardType == CardType.Bomb) {
            List<Card> cardList = getCardList(cardsInHand, nowCards, 4);
            if (Objects.isNull(cardList)) {
                return null;
            }
            //如果是炸弹直接返回
            if (cardList.size() == 4 || cardList.size() == 2) {
                return cardList;
            }
            return null;
        }
        if (cardType == CardType.Straight) {
            int cardSize = nowCards.size();
            Optional<Card> max = nowCards.stream().max(Comparator.comparingInt(c -> c.cardPoint));
            Card maxCard = max.get();
            //判断有没有比它大的顺子
            //再判断有没有炸弹
            return getBigStraight(cardsInHand, maxCard.cardPoint, cardSize);
        }
        if (cardType == CardType.plane) {
            //TODO
            return null;
        }
        return null;
    }

    private List<Card> getBigStraight(List<Card> cardsInHand, int maxCardPoint, int cardSize) {
        //判断有没有比它大的顺子
        Optional<Card> min = cardsInHand.stream()
                .filter(card -> card.cardPoint > maxCardPoint)
                .filter(card -> card.cardPoint != 16 && card.cardPoint != 17)
                .min(Comparator.comparingInt(c -> c.cardPoint));
        if (!min.isPresent()) {
            //判断剩下的牌是否为炸弹
            if (isCommonBoom(cardsInHand)) {
                return cardsInHand;
            }
            if (isKingBoom(cardsInHand)) {
                return cardsInHand;
            }
        } else {
            List<Card> cards = new ArrayList<>();
            for (int i = 1; i < cardSize; i++) {
                int cardPoint = min.get().cardPoint - i;
                List<Card> collect = cardsInHand.stream()
                        .filter(card -> card.cardPoint == cardPoint)
                        .filter(card -> card.cardPoint != 16 && card.cardPoint != 17)
                        .limit(1).collect(Collectors.toList());
                if (collect.isEmpty()) {
                    return getBigStraight(cardsInHand, maxCardPoint + 1, cardSize);
                }
                cards.add(collect.get(0));
            }
            cards.add(min.get());
            return cards;
        }
        //炸弹
        Optional<Card> min2 = cardsInHand.stream()
                .collect(Collectors.groupingBy(card -> card.cardPoint))
                .values()
                .stream()
                .filter(cards -> cards.size() == 4)
                .map(cards -> cards.get(0))
                .min(Comparator.comparingInt(card -> card.cardPoint));
        if (min2.isPresent()) {
            List<Card> collect = cardsInHand.stream()
                    .filter(card -> card.cardPoint == min2.get().cardPoint)
                    .limit(4).collect(Collectors.toList());
            return collect;
        } else {
            //找到王炸
            List<Card> collect = cardsInHand.stream()
                    .filter(card -> card.cardPoint == 16 || card.cardPoint == 17)
                    .limit(2).collect(Collectors.toList());
            if (collect.size() == 2) {
                return collect;
            }
            return null;
        }
    }

    private List<Card> getCardList(List<Card> cardsInHand, List<Card> nowCards, int cardSize) {
        //找比当期牌大的对子
        Optional<Card> min = cardsInHand.stream()
                .filter(card -> card.cardPoint > nowCards.get(0).cardPoint)
                .collect(Collectors.groupingBy(card -> card.cardPoint))
                .values()
                .stream()
                .filter(cards -> cards.size() == cardSize)
                .map(cards -> cards.get(0))
                .min(Comparator.comparingInt(card -> card.cardPoint));
        if (min.isPresent()) {
            //找到两张对子或者王炸
            List<Card> collect = cardsInHand.stream()
                    .filter(card -> card.cardPoint == min.get().cardPoint)
                    .limit(cardSize).collect(Collectors.toList());
            return collect;
        }
        //找四张炸弹
        else {
            Optional<Card> min2 = cardsInHand.stream()
                    .filter(card -> card.cardPoint > nowCards.get(0).cardPoint)
                    .collect(Collectors.groupingBy(card -> card.cardPoint))
                    .values()
                    .stream()
                    .filter(cards -> cards.size() == 4)
                    .map(cards -> cards.get(0))
                    .min(Comparator.comparingInt(card -> card.cardPoint));
            if (min2.isPresent()) {
                List<Card> collect = cardsInHand.stream()
                        .filter(card -> card.cardPoint == min2.get().cardPoint)
                        .limit(4).collect(Collectors.toList());
                return collect;
            } else {
                //找到王炸
                List<Card> collect = cardsInHand.stream()
                        .filter(card -> card.cardPoint == 16 || card.cardPoint == 17)
                        .limit(2).collect(Collectors.toList());
                if (collect.size() == 2) {
                    return collect;
                }
                return null;
            }
        }
    }
//TODO 地主牌还没分配

    /**
     * 查找指定张数的牌有哪些，如：cardSize=2，33，44，55；cardSize=2，333，444，555；
     * @param cardsInHand 手上的牌
     * @param cardSize 指定的张数
     */
  private Optional<List<List<Card>>> findTheSameCardPointWithSize(List<Card> cardsInHand, int cardSize) {
        List<List<Card>> result = new ArrayList<>();
        cardsInHand = cardsInHand.stream()
                .sorted(Comparator.comparingInt(card -> card.cardSort))
                .sorted(Comparator.comparingInt(card -> card.cardPoint))
                .collect(Collectors.toList());
        switch (cardSize) {
            case 1:{
                for (Card card : cardsInHand) {
                    List<Card> item = new ArrayList<>();
                    item.add(card);
                    result.add(item);
                }
                break;
            }
            case 2:{
                for (int i = 0; i < cardsInHand.size(); i++) {
                    Card card = cardsInHand.get(i);
                    if (card.cardPoint == 16 || card.cardPoint == 17) {
                        continue;
                    }
                    if (i + 1 < cardsInHand.size()) {
                        Card nextCard = cardsInHand.get(i + 1);
                        if (card.cardPoint == nextCard.cardPoint) {
                            List<Card> item = new ArrayList<>();
                            item.add(card);
                            item.add(nextCard);
                            result.add(item);
                            i += 2;
                        }
                    }
                }
                break;
            }
            case 3:{
                for (int i = 0; i < cardsInHand.size(); i++) {
                    Card card = cardsInHand.get(i);
                    if (i + 2 < cardsInHand.size()) {
                        Card nextCard = cardsInHand.get(i + 1);
                        Card nextNextCard = cardsInHand.get(i + 2);
                        if (card.cardPoint == nextCard.cardPoint
                                && card.cardPoint == nextNextCard.cardPoint) {
                            List<Card>item = new ArrayList<>();
                            item.add(card);
                            item.add(nextCard);
                            item.add(nextNextCard);
                            result.add(item);
                            i += 3;
                        }
                    }
                }
                break;
            }
            case 4:{
                for (int i = 0; i < cardsInHand.size(); i++) {
                    Card card = cardsInHand.get(i);
                    if (i + 3 < cardsInHand.size()) {
                        Card nextCard = cardsInHand.get(i + 1);
                        Card nextNextCard = cardsInHand.get(i + 2);
                        Card lastCard = cardsInHand.get(i + 3);
                        if (card.cardPoint == nextCard.cardPoint
                                && nextCard.cardPoint == nextNextCard.cardPoint
                                && nextNextCard.cardPoint == lastCard.cardPoint) {
                            List<Card>item = new ArrayList<>();
                            item.add(card);
                            item.add(nextCard);
                            item.add(nextNextCard);
                            item.add(lastCard);
                            result.add(item);
                            i += 4;
                        }
                    }
                }
                break;
            }
        }
        return Optional.of(result);
    }

    /**
     * 查找最大顺子;12345
     *                345 78910JQK
     * @param cardsInHand 手上的牌
     */
    private Optional<List<Card>> findMaxStraight(List<Card> cardsInHand) {
        cardsInHand = cardsInHand.stream()
                .filter(card -> card.cardPoint != 16 && card.cardPoint != 17)
                .sorted(Comparator.comparingInt(card -> card.cardSort))
                .sorted(Comparator.comparingInt(card -> card.cardPoint))
                .collect(Collectors.toList());
        List<Card> result = new ArrayList<>();
        List<Card> temp = new ArrayList<>();
        int index = 0;
        boolean b = true;
        while (index < cardsInHand.size()) {
            Card card = cardsInHand.get(index);
            if (index + 1 < cardsInHand.size()) {
                Card nextCard = cardsInHand.get(index + 1);
                if (card.cardPoint == nextCard.cardPoint - 1) {
                    if (b){
                        temp.add(card);
                        b = false;
                    }
                    temp.add(nextCard);
                    if (temp.size() >= 5 && temp.size() > result.size()) {
                        result.clear();
                        result.addAll(temp);
                    }
                    index ++;
                } else if (card.cardPoint == nextCard.cardPoint) {
                    index++;
                } else {
                    if (temp.size() < 5) {
                        temp.clear();
                    } else {
                        if (temp.size() >= result.size()) {
                            result.clear();
                            result.addAll(temp);
                            temp.clear();
                        }
                    }
                    b = true;
                    index++;
                }
            } else {
                if (temp.size() < 5) {
                    temp.clear();
                } else {
                    if (temp.size() > result.size()) {
                        result = temp;
                    }
                    temp.clear();
                }
                index ++;
            }
        }
        return Optional.of(result);
    }

//    public static void main(String[] args) {
//        //String json = "[{\"cardPoint\":3,\"cardSort\":0},{\"cardPoint\":3,\"cardSort\":1},{\"cardPoint\":3,\"cardSort\":2},{\"cardPoint\":4,\"cardSort\":3},{\"cardPoint\":5,\"cardSort\":0},{\"cardPoint\":5,\"cardSort\":2},{\"cardPoint\":6,\"cardSort\":1},{\"cardPoint\":7,\"cardSort\":2},{\"cardPoint\":8,\"cardSort\":2},{\"cardPoint\":11,\"cardSort\":0},{\"cardPoint\":12,\"cardSort\":0},{\"cardPoint\":12,\"cardSort\":2},{\"cardPoint\":12,\"cardSort\":3},{\"cardPoint\":13,\"cardSort\":0},{\"cardPoint\":13,\"cardSort\":1},{\"cardPoint\":14,\"cardSort\":1},{\"cardPoint\":14,\"cardSort\":2}]";
//        //Optional<List<Card>> optionalCards2 = findMaxStraight(JSON.parseArray(json, Card.class));
//
//        List<Card> cards = Card.newCards();
//        int offset = 0;
//        //地主牌
//        cards.subList(offset, 3).toArray(new Card[0]);
//        offset += 3;
//        //发牌每人17张
//        for (int i = 0; i < 3; i++) {
//            List<Card> cardsInHand = new ArrayList<>(cards.subList(offset, offset + 17));
//            Optional<List<Card>> optionalCards = findMaxStraight(cardsInHand);
//            offset += 17;
//        }
//    }

}
