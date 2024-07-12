package com.hbsoo.server.message.entity;

import com.hbsoo.server.session.UserSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 json ClientPacket {
 "header":{" byte[]  " : "  [customize bytes]"},
 "bodyLen":{" int  " : "  [4 bytes]"},
 "rawBodyLen":{" int  " : "  [4 bytes]"},
 "msgType":{" int  " : "  [4 bytes]"},
 "body": [
         {" boolean  " : "  [1 byte]"},
         {" byte  " : "  [1 byte]"},
         {" short  " : "  [2 bytes]"},
         {" int  " : "  [4 bytes]"},
         {" long  " : "  [8 bytes]"},
         {" float  " : "  [4 bytes]"},
         {" double  " : "  [8 bytes]"},
         {" string  " : "  [int 4 bytes + strLen bytes]"},
         {" byte[]  " : "  [int 4 bytes + bytes]"}
    ]
 }

 json NetworkPacket {
 "header":{" byte[]  " : "  [customize bytes]"},
 "bodyLen":{" int  " : "  [4 bytes]"},
 "rawBodyLen":{" int  " : "  [4 bytes]"},
 "msgType":{" int  " : "  [4 bytes]"},
 "body": {
     "rawBody":
         [
             {" boolean  " : "  [1 byte]"},
             {" byte  " : "  [1 byte]"},
             {" short  " : "  [2 bytes]"},
             {" int  " : "  [4 bytes]"},
             {" long  " : "  [8 bytes]"},
             {" float  " : "  [4 bytes]"},
             {" double  " : "  [8 bytes]"},
             {" string  " : "  [int 4 bytes + strLen bytes]"},
             {" byte[]  " : "  [int 4 bytes + bytes]"}
         ],
     "expandBody":
         {
             "msgId":{" long  " : "  [8 bytes]"},
             ' 0:tcp,1:udp,2:binary_websocket,3:text_websocket,4:http
             "protocolType": {" byte  " : "  [1 byte]"},
             "fromServerId  " : {" int  " : "  [4 bytes]"},
             "fromServerType  " :   {" string  " : "  [int 4 bytes + strLen bytes]"},
             "userChannelId  " :   {" string  " : "  [int 4 bytes + strLen bytes]"},
             "isLogin" : {" byte  " : "  [1 byte]"},
             "userId":{" long  " : "  [8 bytes]"},
             "userSession" : "  UserSession "
         }
     }
 }

 json UserSession {
 "id": {" long  " : "  [8 bytes]"},
 "belongServer.host": {" string  " : "  [int 4 bytes + strLen bytes]"},
 "belongServer.port": {" int  " : "  [4 bytes]"},
 "belongServer.type": {" string  " : "  [int 4 bytes + strLen bytes]"},
 "belongServer.id": {" int  " : "  [4 bytes]"},
 "belongServer.weight": {" int  " : "  [4 bytes]"},
 "belongServer.clientSize": {" int  " : "  [4 bytes]"},
 "isUdp": {" byte  " : "  [1 byte]"},
 "udpHost": {" string  " : "  [int 4 bytes + strLen bytes]"},
 "udpPort": {" int  " : "  [4 bytes]"},
 "channelId": {" string  " : "  [int 4 bytes + strLen bytes]"}
 }
 */
public final class NetworkPacket {

    public static byte[] TCP_HEADER = new byte[]{'T', 'H', 'B', 'S'};
    public static byte[] UDP_HEADER = new byte[]{'U', 'H', 'B', 'S'};
    public static void setTcpHeader(byte[] header) {
        TCP_HEADER = header;
    }
    public static void setUdpHeader(byte[] header) {
        UDP_HEADER = header;
    }

    public static class Builder {
        private final AtomicInteger allBodyLength = new AtomicInteger(0);
        private final List<Byte> rawBodyByteList = new ArrayList<>();
        private final List<Byte> expandBodyList = new ArrayList<>();
        private final List<Byte> headerByteList = new ArrayList<>();
        private Integer msgType;
        private boolean isWriteRawBody = true;

        private Builder(byte[] header) {
            if (!(header.length % 2 == 0)) {
                throw new RuntimeException("header.length % 2 must equal 0");
            }
            for (byte aByte : header) {
                this.headerByteList.add(aByte);
            }
            //this.packageLength.getAndAdd(header.length);
        }

        public static Builder withDefaultHeader() {
            return withHeader(TCP_HEADER);
        }

        public static Builder withHeader(byte[] header) {
            return new Builder(header);
        }

        public byte[] getHeader() {
            ByteBuffer buffer = ByteBuffer.allocate(headerByteList.size());
            headerByteList.forEach(buffer::put);
            return buffer.array();
        }

        public Builder writeExpandBodyMode() {
            this.isWriteRawBody = false;
            return this;
        }
        public Builder writeRawBodyMode() {
            this.isWriteRawBody = true;
            return this;
        }

        public boolean isWriteRawBody() {
            return isWriteRawBody;
        }

        public Builder writeByte(byte b) {
            if (isWriteRawBody) {
                rawBodyByteList.add(b);
            } else {
                expandBodyList.add(b);
            }
            allBodyLength.incrementAndGet();
            return this;
        }
        public Builder writeBoolean(boolean b) {
            if (isWriteRawBody) {
                rawBodyByteList.add((byte) (b ? 1 : 0));
            } else {
                expandBodyList.add((byte) (b ? 1 : 0));
            }
            allBodyLength.incrementAndGet();
            return this;
        }

        public Builder writeBytes(byte[] bytes) {
            final ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(bytes.length);
            final byte[] array = buffer.array();
            for (byte b : array) {
                if (isWriteRawBody) {
                    rawBodyByteList.add(b);
                } else {
                    expandBodyList.add(b);
                }
            }
            allBodyLength.getAndAdd(4);
            for (byte aByte : bytes) {
                if (isWriteRawBody) {
                    rawBodyByteList.add(aByte);
                } else {
                    expandBodyList.add(aByte);
                }
            }
            allBodyLength.getAndAdd(bytes.length);
            return this;
        }

        public Builder writeShort(short... shorts) {
            for (short aInt : shorts) {
                final ByteBuffer buffer = ByteBuffer.allocate(2);
                buffer.putShort(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(b);
                    } else {
                        expandBodyList.add(b);
                    }
                }
                allBodyLength.getAndAdd(2);
            }
            return this;
        }

        public Builder msgType(int msgType) {
            this.msgType = msgType;
            return this;
        }

        public Builder writeInt(int... ints) {
            for (int aInt : ints) {
                final ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.putInt(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(b);
                    } else {
                        expandBodyList.add(b);
                    }
                }
                allBodyLength.getAndAdd(4);
            }
            return this;
        }

        public Builder writeFloat(float... floats) {
            for (float aInt : floats) {
                final ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.putFloat(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(b);
                    } else {
                        expandBodyList.add(b);
                    }
                }
                allBodyLength.getAndAdd(4);
            }
            return this;
        }

        public Builder writeLong(long... longs) {
            for (long aInt : longs) {
                final ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putLong(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(b);
                    } else {
                        expandBodyList.add(b);
                    }
                }
                allBodyLength.getAndAdd(8);
            }
            return this;
        }

        public Builder writeDouble(double... doubles) {
            for (double aInt : doubles) {
                final ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putDouble(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(b);
                    } else {
                        expandBodyList.add(b);
                    }
                }
                allBodyLength.getAndAdd(8);
            }
            return this;
        }

        public Builder writeString(String... strings) {
            return writeStr(strings);
        }

        public Builder writeStr(String... strings) {
            for (String str : strings) {
                final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                final ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.putInt(bytes.length);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(b);
                    } else {
                        expandBodyList.add(b);
                    }
                }
                allBodyLength.getAndAdd(4);
                for (byte aByte : bytes) {
                    if (isWriteRawBody) {
                        rawBodyByteList.add(aByte);
                    } else {
                        expandBodyList.add(aByte);
                    }
                }
                allBodyLength.getAndAdd(bytes.length);
            }
            return this;
        }

        public <T extends NetworkPacketEntity<T>> Builder writeObj(T t) {
            t.serializable(this);
            return this;
        }

        public byte[] buildPackage() {
            if (headerByteList.isEmpty()) {
                throw new RuntimeException("package header not set");
            }
            if (Objects.isNull(msgType)) {
                final String name = Thread.currentThread().getName();
                System.out.println("name = " + name);
                throw new RuntimeException("msgType not set");
            }
            int allBodyLen = allBodyLength.get();//包总长度
            int rawBodyLen = rawBodyByteList.size();//原始包长度
            //int expandBodyLen = expandBodyList.size();//扩展包长度
            ByteBuffer targetPacketBuffer = ByteBuffer.allocate(allBodyLen + this.headerByteList.size() + 12);//+12 bodyLen + rawBodyLen + msgType
            headerByteList.forEach(targetPacketBuffer::put);//header
            targetPacketBuffer.putInt(allBodyLen);//bodyLen
            targetPacketBuffer.putInt(rawBodyLen);//rawBodyLen
            targetPacketBuffer.putInt(this.msgType);//msgType
            rawBodyByteList.forEach(targetPacketBuffer::put);//rawBody
            expandBodyList.forEach(targetPacketBuffer::put);//expandBody
            return targetPacketBuffer.array();
        }

        public int msgType() {
            return this.msgType;
        }

        public void sendTcpTo(Channel channel) {
            byte[] bytes = buildPackage();
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        }
        public void sendTcpTo(Channel channel, GenericFutureListener<? extends Future<? super Void>> futureListener) {
            byte[] bytes = buildPackage();
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes)).addListener(futureListener);
        }

        public void sendBinWebSocketTo(Channel channel) {
            byte[] bytes = buildPackage();
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(frame);
        }
        public void sendBinWebSocketTo(Channel channel, GenericFutureListener<? extends Future<? super Void>> futureListener) {
            byte[] bytes = buildPackage();
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(frame).addListener(futureListener);
        }

        public void sendTextWebSocketTo(Channel channel) {
            byte[] bytes = buildPackage();
            TextWebSocketFrame frame = new TextWebSocketFrame(new String(bytes, StandardCharsets.UTF_8));
            channel.writeAndFlush(frame);
        }
        public void sendTextWebSocketTo(Channel channel, GenericFutureListener<? extends Future<? super Void>> futureListener) {
            byte[] bytes = buildPackage();
            TextWebSocketFrame frame = new TextWebSocketFrame(new String(bytes, StandardCharsets.UTF_8));
            channel.writeAndFlush(frame).addListener(futureListener);
        }

        public void sendUdpTo(Channel channel, String host, int port) {
            byte[] bytes = buildPackage();
            DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(bytes),
                    new InetSocketAddress(host, port));
            channel.writeAndFlush(packet);
        }
        public void sendUdpTo(Channel channel, String host, int port, GenericFutureListener<? extends Future<? super Void>> futureListener) {
            byte[] bytes = buildPackage();
            DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(bytes),
                    new InetSocketAddress(host, port));
            channel.writeAndFlush(packet).addListener(futureListener);
        }

        public NetworkPacket.Decoder toDecoder() {
            byte[] bytes = buildPackage();
            return NetworkPacket.Decoder.withHeader(getHeader()).parsePacket(bytes);
        }
    }

    public static class Decoder {
        private final byte[] header;
        private int bodyLen;
        private int rawBodyLen;
        private int msgType;
        private byte[] rawBody;
        private byte[] expandBody;
        private final AtomicInteger rawBodyReadOffset = new AtomicInteger(0);
        private final AtomicInteger expandBodyReadOffset = new AtomicInteger(0);
        private boolean isReadRawBody = true;

        public Decoder readExpandBodyMode() {
            this.isReadRawBody = false;
            return this;
        }
        public Decoder readRawBodyMode() {
            this.isReadRawBody = true;
            return this;
        }

        public boolean isReadRawBody() {
            return isReadRawBody;
        }

        public boolean hasExpandBody() {
            return expandBody != null;
        }

        private Decoder(byte[] header) {
            if (!(header.length % 2 == 0)) {
                throw new RuntimeException("header.length % 2 must equal 0");
            }
            this.header = header;
        }
        public static Decoder withDefaultHeader() {
            return withHeader(NetworkPacket.TCP_HEADER);
        }
        public static Decoder withHeader(byte[] bytes) {
            return new Decoder(bytes);
        }

        public Decoder parsePacket(byte[] received) {
            byte[] readHeader = new byte[this.header.length];
            System.arraycopy(received, 0, readHeader, 0, this.header.length);
            boolean matchHeader = Arrays.equals(this.header, readHeader);
            if (!matchHeader) {
                throw new RuntimeException("header not match");
            }
            byte[] bodyLenBytes = new byte[4];
            int readOffset = this.header.length;
            System.arraycopy(received, readOffset, bodyLenBytes, 0, bodyLenBytes.length);
            int bodyLen = ByteBuffer.wrap(bodyLenBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            readOffset += bodyLenBytes.length;

            byte[] rawBodyLenBytes = new byte[4];
            System.arraycopy(received, readOffset, rawBodyLenBytes, 0, rawBodyLenBytes.length);
            int rawBodyLen = ByteBuffer.wrap(rawBodyLenBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            readOffset += rawBodyLenBytes.length;

            byte[] msgTypeBytes = new byte[4];
            System.arraycopy(received, readOffset, msgTypeBytes, 0, msgTypeBytes.length);
            int msgType = ByteBuffer.wrap(msgTypeBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            readOffset += msgTypeBytes.length;

            byte[] rawBodyBytes = new byte[rawBodyLen];
            System.arraycopy(received, readOffset, rawBodyBytes, 0, rawBodyBytes.length);
            readOffset += rawBodyBytes.length;
            if (bodyLen - rawBodyLen > 0) {
                int expandBodyLen = bodyLen - rawBodyLen;
                byte[] expandBodyBytes = new byte[expandBodyLen];
                System.arraycopy(received, readOffset, expandBodyBytes, 0, expandBodyBytes.length);
                this.expandBody = expandBodyBytes;
            }
            this.bodyLen = bodyLen;
            this.rawBodyLen = rawBodyLen;
            this.msgType = msgType;
            this.rawBody = rawBodyBytes;
            return this;
        }

        public byte[] getHeader() {
            return header;
        }

        public Builder toBuilder() {
            return toBuilder(this.header);
        }
        public Builder toBuilder(byte[] headers) {
            Builder builder = Builder.withHeader(headers);
            byte[] expandBody = this.expandBody;
            byte[] rawBody = this.rawBody;
            int msgType = this.msgType;
            int bodyLen = this.bodyLen;

            builder.allBodyLength.addAndGet(bodyLen);
            builder.msgType = msgType;
            for (byte b : rawBody) {
                builder.rawBodyByteList.add(b);
            }
            if (Objects.nonNull(expandBody)) {
                for (byte b : expandBody) {
                    builder.expandBodyList.add(b);
                }
            }
            return builder;
        }

        /**
         * 利用尚未读取的数据转换成对象
         * @param t 要转换的对象
         * @param <T> 对象类型
         */
        public <T extends NetworkPacketEntity<T>> T decode2Obj(T t) {
            return t.deserialize(this);
        }

        public ExpandBody readExpandBody() {
            ExpandBody expandBody = new ExpandBody();
            expandBody.deserialize(this);
            return expandBody;
        }

        /**
         * 重置已读下标
         */
        public Decoder resetBodyReadOffset() {
            rawBodyReadOffset.set(0);
            expandBodyReadOffset.set(0);
            return this;
        }

        public byte readByte() {
            byte[] bytes = new byte[1];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(1), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(1), bytes, 0, bytes.length);
            }
            return bytes[0];
        }

        public boolean readBoolean() {
            return readByte() == 1;
        }

        public byte[] readBytes() {
            final int len = readInt();
            byte[] bytes = new byte[len];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(len), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(len), bytes, 0, bytes.length);
            }
            return bytes;
        }

        public short readShort() {
            byte[] bytes = new byte[2];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(2), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(2), bytes, 0, bytes.length);
            }
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
        }

        public int readInt() {
            byte[] bytes = new byte[4];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(4), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(4), bytes, 0, bytes.length);
            }
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        }
        public float readFloat() {
            byte[] bytes = new byte[4];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(4), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(4), bytes, 0, bytes.length);
            }
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
        }

        /**
         * 获取消息类型,阅读偏移量不移动
         */
        public int getMsgType() {
            return this.msgType;
        }

        /**
         * 获取消息类型,阅读偏移量移动4个字节
         */
        public int readMsgType() {
            return this.msgType;
        }

        public long readLong() {
            byte[] bytes = new byte[8];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(8), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(8), bytes, 0, bytes.length);
            }
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
        }

        public double readDouble() {
            byte[] bytes = new byte[8];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(8), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(8), bytes, 0, bytes.length);
            }
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getDouble();
        }

        public String readStr() {
            final byte[] bytes = readBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }

        /**
         * 读取所有剩下的数据
         */
        public byte[] readAllTheRestBodyData() {
            int offset = isReadRawBody ? rawBodyReadOffset.get() : expandBodyReadOffset.get();
            int restDataLen = (isReadRawBody ? rawBody.length : expandBody.length) - offset;
            byte[] bytes = new byte[restDataLen];
            if (isReadRawBody) {
                System.arraycopy(rawBody, rawBodyReadOffset.getAndAdd(restDataLen), bytes, 0, bytes.length);
            } else {
                System.arraycopy(expandBody, expandBodyReadOffset.getAndAdd(restDataLen), bytes, 0, bytes.length);
            }
            return bytes;
        }

        public byte skipGetByte(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[1], skips);
            return bytes[0];
        }
        public boolean skipGetBoolean(DecodeSkip... skips) {
            return skipGetByte(skips) == 1;
        }
        public short skipGetShort(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[2], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
        }
        public int skipGetInt(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[4], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        }
        public float skipGetFloat(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[4], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
        }
        public long skipGetLong(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[8], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
        }
        public double skipGetDouble(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[8], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getDouble();
        }
        public byte[] skipGetBytes(DecodeSkip... skips) {
            final int dataLen = skipGetInt(skips);
            DecodeSkip[] sk = new DecodeSkip[skips.length + 1];
            for (int i = 0; i < skips.length; i++) {
                sk[i] = skips[i];
            }
            sk[sk.length - 1] = DecodeSkip.INT;
            byte[] bytes = skipGet(new byte[dataLen], sk);
            return bytes;
        }
        public String skipGetStr(DecodeSkip... skips) {
            final byte[] bytes = skipGetBytes(skips);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private byte[] skipGet(byte[] targetBytes, DecodeSkip... skips) {
            int offset = getOffset(skips);
            byte [] body = isReadRawBody ? rawBody : expandBody;
            System.arraycopy(body, offset, targetBytes, 0, targetBytes.length);
            return targetBytes;
        }

        private int getOffset(DecodeSkip[] skips) {
            if (skips == null) {
                return 0;
            }
            int offset = 0;
            for (DecodeSkip skip : skips) {
                switch (skip) {
                    case BYTE: offset += 1;
                        break;
                    case SHORT: offset += 2;
                        break;
                    case INT: offset += 4;
                        break;
                    case LONG: offset += 8;
                        break;
                    case BYTE_ARRAY:
                    case STRING: {
                        byte[] bytes = new byte[4];
                        byte [] body = isReadRawBody ? rawBody : expandBody;
                        System.arraycopy(body, offset, bytes, 0, bytes.length);
                        int len = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
                        offset += (4 + len);
                        break;
                    }
                }
            }
            return offset;
        }

    }

    public enum DecodeSkip {
        BYTE,
        SHORT,
        INT,
        LONG,
        BYTE_ARRAY,
        STRING,
    }

}
