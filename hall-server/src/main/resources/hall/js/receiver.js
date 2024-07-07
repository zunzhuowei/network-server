var receiver;
(function () {
    receiver = {
        onMsg(data) {
            //debugger;
            var parser = new MsgParser(data);
            var dv = parser.dataView();
            var msgType = parser.msgType();
            router.route(msgType, dv);
        },
        onOpen(evt) {
            var username = $('#username').text();
            var buffer = sender.buildHead(100)
                .buildString(username)
                .finish();
            socket.send(buffer);
        },
        onError(evt) {
            alert("Socket发生了错误");
        },
        onClose(evt) {
            console.log("Socket已关闭");
        },
    }
})();

// 消息解析者
function MsgParser(data) {
    this.data = data;
    this.msgType = function () {//header + bodyLen
        return this.dataView.getInt32(4 + 4, socketBuilder.littleEndian);
    };
    this.dataView = function () {
        return this.dataView = new DataView(this.data);
    };
}

// 拆包工具类
function DataParser(dv, position) {
    this.dataView = dv;
    this.position = position === undefined ? 12 : position; //header + bodyLen + msgType
    this.textDecoder = new TextDecoder();

    this.getStr = function getStr() {
        var len = this.getInt();
        return this.getString(len);
    };
    this.getString = function getString(str_len) {
        var temp = [];
        for (let i = 0; i < str_len; i++) {
            var b = this.dataView.getInt8(this.position);
            temp.push(b);
            this.position++;
        }
        let bytes = new Uint8Array(temp);
        return this.textDecoder.decode(bytes);
    };
    this.getLong = function getLong() {
        var longV = this.dataView.getBigInt64(this.position, socketBuilder.littleEndian);
        this.position += 8;
        return longV;
    };
    this.getInt = function getInt() {
        var intV = this.dataView.getInt32(this.position, socketBuilder.littleEndian);
        this.position += 4;
        return intV;
    };
    this.getShort = function getShort() {
        var intV = this.dataView.getInt16(this.position, socketBuilder.littleEndian);
        this.position += 2;
        return intV;
    };
    this.getByte = function getByte() {
        var intV = this.dataView.getInt8(this.position);
        this.position += 1;
        return intV;
    };
    this.getInt64 = function getInt64() {
        return this.getLong();
    };
    this.getInt32 = function getInt32() {
        return this.getInt();
    };
    this.getInt16 = function getInt16() {
        return this.getShort();
    };
    this.getInt8 = function getInt8() {
        return this.getByte();
    };
    this.getFloat = function getFloat() {
        var floatV = this.dataView.getFloat32(this.position, socketBuilder.littleEndian);
        this.position += 4;
        return floatV;
    }
    this.getDouble = function getDouble() {
        var doubleV = this.dataView.getFloat64(this.position, socketBuilder.littleEndian);
        this.position += 8;
        return doubleV;
    }
}
