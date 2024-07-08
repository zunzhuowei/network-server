var router = {};
router.route = function (msgType, dataView) {
    console.log("msgType -----::" + msgType);
    var dataParser = new DataParser(dataView);
    var result = router['route_' + msgType](dataParser);
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
    console.log("userId:" + userId + ";username:" + username)
    $("#chat-list").append("<li>[" + username + "]:" + userId + "上线了！</li>");
};

// 心跳包
router.route_99 = function (dataParser) {
    var userId = dataParser.getLong();
    console.log("心跳包 userId:" + userId)
};

//加入房间消息
router.route_101 = function (dataParser) {
    var chatRoomJson = dataParser.getStr();
    console.log("chatRoomJson:" + chatRoomJson)
    $("#chat-list").append("<li>" + chatRoomJson + "</li>");
};
//聊天消息
router.route_1001 = function (dataParser) {
    var userId = dataParser.getLong();
    var message = dataParser.getStr();
    $("#chat-list").append("<li>[" + userId + "]:" + message + "</li>");
};