var router = {};
router.route = function (msgType, dataView) {
    var dataParser = new DataParser(dataView);
    try {
        var result = router['route_' + msgType](dataParser);
    }catch (e) {
        console.log("msgType -----::" + msgType);
        throw e;
    }

    //var time = (new Date()).Format("yyyy-MM-dd hh:mm:ss");
    //if (msgType === 10006) { //心跳
    //    var num = $('#heartbeat').text();
    //    $('#heartbeat').text(++num);
    //} else {
    //    $('#greetings').append("<li>[" + time + "]命令:" + cmd + " | 结果: " + result + "</li>");
    //}
};

// 登录
router.route_100 = function (dataParser) {
    var username = dataParser.getStr();
    var userId = dataParser.getLong();
    $("#chat-list").append("<li>[" + username + "]:" + userId + "上线了！</li>");
};

// 心跳包
router.route_99 = function (dataParser) {
    var userId = dataParser.getLong();
    //console.log("心跳包 userId:" + userId)
};

//加入房间消息
router.route_101 = function (dataParser) {
    var chatRoomJson = dataParser.getStr();
    //console.log("chatRoomJson:" + chatRoomJson)
    $("#chat-list").append("<li>" + chatRoomJson + "</li>");
};
//退出房间消息
router.route_102 = function (dataParser) {
    var userId = dataParser.getLong();
    var msg = dataParser.getStr();
    $("#chat-list").append("<li>[" + userId + "]:" + msg + "</li>");
};
//手牌消息
router.route_103 = function (dataParser) {
    var cards = dataParser.getStr();
    var parse = JSON.parse(cards);
    var cs = parse.map(function (card) {
        return cardMapping(card);
    });
    $("#chat-list").append("<li>" + JSON.stringify(cs) + "</li>");
};
//出牌消息
router.route_1001 = function (dataParser) {
    var discardCards = dataParser.getStr();
    var parse = JSON.parse(discardCards);
    var cs = parse.map(function (card) {
        return cardMapping(card);
    });
    var turnNo = dataParser.getInt();
    var cardSize = dataParser.getInt();
    $("#chat-list").append("<li>出的牌:" + JSON.stringify(cs) + "--接下来轮到谁出牌:" + turnNo + "--出牌人手上还有多少牌:" + cardSize + "</li>");
};
//赢牌消息
router.route_1002 = function (dataParser) {
    var userId = dataParser.getLong();
    var message = dataParser.getStr();
    $("#chat-list").append("<li>[" + userId + "]赢了,房间号:" + message + "</li>");
};
//要不起
router.route_1003 = function (dataParser) {
    var userId = dataParser.getLong();
    var message = dataParser.getStr();
    $("#chat-list").append("<li>[" + userId + "]:" + message + "</li>");
};
//倒计时
router.route_1004 = function (dataParser) {
    var timer = dataParser.getInt();
    if (timer == 10) {
        $("#chat-list").append("<li>[" + "倒计时：" + "]:" + timer + "</li>");
    } else {
        $("#chat-list > li:last").append("," + timer);
    }
};
//重新开始游戏
router.route_1005 = function (dataParser) {
    $("#chat-list").empty();//删除所有子节点
};

router.onOpen = function () {
    var roomName = $('#roomName').text();
    var username = $('#username').text();
    var buffer = sender.buildHead(100)
        .buildString(username)
        .buildString(roomName)
        .finish();
    socket.send(buffer);
};

/**
 * 牌点数映射
 * @param card
 * @returns {string|*|string}
 */
function cardMapping(card) {
    var cardPoint = card.cardPoint;
    var cardSort = card.cardSort;
    var num;
    switch (cardPoint) {
        case 3:
            num = "3";
            break;
        case 4:
            num = "4";
            break;
        case 5:
            num = "5";
            break;
        case 6:
            num = "6";
            break;
        case 7:
            num = "7";
            break;
        case 8:
            num = "8";
            break;
        case 9:
            num = "9";
            break;
        case 10:
            num = "10";
            break;
        case 11:
            num = "J";
            break;
        case 12:
            num = "Q";
            break;
        case 13:
            num = "K";
            break;
        case 14:
            num = "A";
            break;
        case 15:
            num = "2";
            break;
        case 16:
            num = "小王";
            break;
        case 17:
            num = "大王";
            break;
        default:
            return "未知";
    }
    switch (cardSort) {
        case 0:
            return num !== "小王" && num !== "大王" ? "方块:" + num : num;
        case 1:
            return "梅花:" + num;
        case 2:
            return "红桃:" + num;
        case 3:
            return "黑桃:" + num;
        default:
            return "未知";
    }
    return num;
}