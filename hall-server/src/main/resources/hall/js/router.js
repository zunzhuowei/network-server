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
    var time = (new Date()).Format("yyyy-MM-dd hh:mm:ss");
    $("#chat-list").append("<li>" + time + "[" + username + "]:" + userId + "上线了！</li>");
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
    $("#chat-list").append("<li>" + cards + "</li>");
};
//出牌消息
router.route_1001 = function (dataParser) {
    var discardCards = dataParser.getStr();
    var turnNo = dataParser.getInt();
    var cardSize = dataParser.getInt();
    $("#chat-list").append("<li>" + discardCards + "--" + turnNo + "--" + cardSize + "</li>");
};
//赢牌消息
router.route_1002 = function (dataParser) {
    var userId = dataParser.getLong();
    var message = dataParser.getStr();
    $("#chat-list").append("<li>[" + userId + "]:" + message + "</li>");
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