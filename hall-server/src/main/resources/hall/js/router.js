var router = {};
router.route = function (msgType, dataView) {
    console.log("msgType -----::" + msgType);
    var result = router['route' + msgType](dataView);
    //var time = (new Date()).Format("yyyy-MM-dd hh:mm:ss");
    //if (msgType === 10006) { //心跳
    //    var num = $('#heartbeat').text();
    //    $('#heartbeat').text(++num);
    //} else {
    //    $('#greetings').append("<li>[" + time + "]命令:" + cmd + " | 结果: " + result + "</li>");
    //}
};

// 登录
router.route100 = function (dataView) {
    var dataParser = new DataParser(dataView);
    var username = dataParser.getStr();
    var userId = dataParser.getLong();
    console.log("userId:" + userId + ";username:" + username)
};

// 心跳包
router.route99 = function (dataView) {
    var dataParser = new DataParser(dataView);
    var userId = dataParser.getLong();
    console.log("心跳包 userId:" + userId)
};

