var sender;
(function () {
    sender = {
        littleEndian: false,
        position: 0,
        dataView: {},
        msgType: 0,
        textEncoder: new TextEncoder(),
        buildUint64(uint64) {
            this.dataView.setBigUint64(this.position, uint64, this.littleEndian);
            this.position += 8;
            return this;
        },
        buildInt64(int64) {
            this.dataView.setBigInt64(this.position, int64, this.littleEndian);
            this.position += 8;
            return this;
        },
        buildUint32(uint32) {
            this.dataView.setUint32(this.position, uint32, this.littleEndian);
            this.position += 4;
            return this;
        },
        buildInt32(int32) {
            this.dataView.setInt32(this.position, int32, this.littleEndian);
            this.position += 4;
            return this;
        },
        buildUint16(uint16) {
            this.dataView.setUint16(this.position, uint16, this.littleEndian);
            this.position += 2;
        },
        buildInt16(int16) {
            this.dataView.setInt16(this.position, int16, this.littleEndian);
            this.position += 2;
        },
        buildUint8(uint8) {
            this.dataView.setUint8(this.position, uint8);
            this.position += 1;
            return this;
        },
        buildInt8(int8) {
            this.dataView.setInt8(this.position, int8);
            this.position += 1;
            return this;
        },
        buildChar(char) {
            char = this.textEncoder.encode(char);
            this.dataView.setUint16(this.position, char, this.littleEndian);
            this.position += 2;
            return this;
        },
        buildString(string) {
            string = this.textEncoder.encode(string);
            this.dataView.setUint32(this.position, string.length, this.littleEndian);
            this.position += 4;
            for (let i = 0; i < string.length; i++) {
                // str value
                this.dataView.setUint8(this.position, string[i]);
                this.position++;
            }
            return this;
        },
        buildFloat(float) {
            this.dataView.setFloat32(this.position, float, this.littleEndian);
            this.position += 4;
            return this;
        },
        buildDouble(double) {
            this.dataView.setFloat64(this.position, double, this.littleEndian);
            this.position += 8;
            return this;
        },
        finish() {
            this.dataView.setUint8(0, this.textEncoder.encode("T"));
            this.dataView.setUint8(1, this.textEncoder.encode("H"));
            this.dataView.setUint8(2, this.textEncoder.encode("B"));
            this.dataView.setUint8(3, this.textEncoder.encode("S"));
            this.dataView.setUint32(4, this.position - 8, this.littleEndian); // body len
            return this.dataView.buffer.slice(0, this.position);
        },
        buildHead(msgType) {
            this.position = 0;
            this.msgType = 0;
            this.dataView = null;

            var buf = new ArrayBuffer(1024);
            this.dataView = new DataView(buf);
            this.buildInt32(0);//header
            this.buildInt32(0);//bodyLen
            this.buildInt32(msgType);//msgType
            return this;
        },

    }
})();
