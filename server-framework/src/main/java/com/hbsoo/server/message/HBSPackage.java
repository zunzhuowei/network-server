package com.hbsoo.server.message;

import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 消息包
 * 包结构：header(4 byte) + packageBodyLen(4 int) + body(msgType(4 int) + (n-4))
 * Created by zun.wei on 2024/6/4.
 */
public final class HBSPackage {

    public static final byte[] TCP_HEADER = new byte[]{'T', 'H', 'B', 'S'};
    public static final byte[] UDP_HEADER = new byte[]{'U', 'H', 'B', 'S'};

    public static class Builder {
        private final AtomicInteger packageLength = new AtomicInteger(0);
        private final List<Byte> bodyByteList = new ArrayList<>();
        private final List<Byte> headerByteList = new ArrayList<>();
        private final List<Byte> msgTypeList = new ArrayList<>();

        private Builder(byte[] header) {
            assert header.length % 2 == 0;
            for (byte aByte : header) {
                this.headerByteList.add(aByte);
            }
            this.packageLength.getAndAdd(header.length);
        }

        public static Builder withDefaultHeader() {
            return withHeader(TCP_HEADER);
        }

        public static Builder withHeader(byte[] header) {
            return new Builder(header);
        }

        public byte[] getHeader() {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            headerByteList.forEach(buffer::put);
            return buffer.array();
        }

        public Builder writeByte(byte b) {
            bodyByteList.add(b);
            packageLength.incrementAndGet();
            return this;
        }

        public Builder writeBytes(byte[] bytes) {
            final ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(bytes.length);
            final byte[] array = buffer.array();
            for (byte b : array) {
                bodyByteList.add(b);
            }
            packageLength.getAndAdd(4);
            for (byte aByte : bytes) {
                bodyByteList.add(aByte);
            }
            packageLength.getAndAdd(bytes.length);
            return this;
        }

        public Builder writeShort(short... ints) {
            for (short aInt : ints) {
                final ByteBuffer buffer = ByteBuffer.allocate(2);
                buffer.putShort(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    bodyByteList.add(b);
                }
                packageLength.getAndAdd(2);
            }
            return this;
        }

        public Builder msgType(int msgType) {
            msgTypeList.clear();
            final ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(msgType);
            final byte[] array = buffer.array();
            for (byte b : array) {
                msgTypeList.add(b);
            }
            packageLength.getAndAdd(4);
            return this;
        }

        public Builder writeInt(int... ints) {
            for (int aInt : ints) {
                final ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.putInt(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    bodyByteList.add(b);
                }
                packageLength.getAndAdd(4);
            }
            return this;
        }

        public Builder writeLong(long... ints) {
            for (long aInt : ints) {
                final ByteBuffer buffer = ByteBuffer.allocate(8);
                buffer.putLong(aInt);
                final byte[] array = buffer.array();
                for (byte b : array) {
                    bodyByteList.add(b);
                }
                packageLength.getAndAdd(8);
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
                    bodyByteList.add(b);
                }
                packageLength.getAndAdd(4);
                for (byte aByte : bytes) {
                    bodyByteList.add(aByte);
                }
                packageLength.getAndAdd(bytes.length);
            }
            return this;
        }

        public Builder writeObj(Object... objects) {
            for (Object object : objects) {
                if (object instanceof Byte) {
                    writeByte((byte) object);
                    continue;
                }
                if (object instanceof byte[]) {
                    writeBytes((byte[]) object);
                    continue;
                }
                if (object instanceof Short) {
                    writeShort((short) object);
                    continue;
                }
                if (object instanceof Integer) {
                    writeInt((int) object);
                    continue;
                }
                if (object instanceof Long) {
                    writeLong((long) object);
                    continue;
                }
                if (object instanceof String) {
                    writeStr((String) object);
                    continue;
                }
                Gson gson = new Gson();
                final String json = gson.toJson(object);
                writeStr(json);
            }
            return this;
        }

        public byte[] buildPackage() {
            if (headerByteList.isEmpty()) {
                throw new RuntimeException("package header not set");
            }
            final boolean empty = msgTypeList.isEmpty();
            if (empty) {
                throw new RuntimeException("msgType not set");
            }
            int packageLen = packageLength.get();
            int packageBodyLen = packageLen - headerByteList.size();
            final ByteBuffer buffer = ByteBuffer.allocate(packageLen + 4);
            // header +(int) bodyLen + body
            headerByteList.forEach(buffer::put);
            buffer.putInt(packageBodyLen);
            msgTypeList.forEach(buffer::put);
            bodyByteList.forEach(buffer::put);
            return buffer.array();
        }

        public int msgType() {
            byte[] msgTypeBytes = new byte[msgTypeList.size()];
            for (int i = 0; i < msgTypeList.size(); i++) {
                msgTypeBytes[i] = msgTypeList.get(i);
            }
            return ByteBuffer.wrap(msgTypeBytes).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        public void buildAndSendBytesTo(Channel channel) {
            byte[] bytes = buildPackage();
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
        }
        public void buildAndSendBytesTo(Channel channel, GenericFutureListener<? extends Future<? super Void>> var1) {
            byte[] bytes = buildPackage();
            channel.writeAndFlush(Unpooled.wrappedBuffer(bytes)).addListener(var1);
        }

        public void buildAndSendBinWebSocketTo(Channel channel) {
            byte[] bytes = buildPackage();
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(frame);
        }
        public void buildAndSendBinWebSocketTo(Channel channel, GenericFutureListener<? extends Future<? super Void>> var1) {
            byte[] bytes = buildPackage();
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(frame).addListener(var1);
        }

        public void buildAndSendTextWebSocketTo(Channel channel) {
            byte[] bytes = buildPackage();
            TextWebSocketFrame frame = new TextWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(frame);
        }
        public void buildAndSendTextWebSocketTo(Channel channel, GenericFutureListener<? extends Future<? super Void>> var1) {
            byte[] bytes = buildPackage();
            TextWebSocketFrame frame = new TextWebSocketFrame(Unpooled.wrappedBuffer(bytes));
            channel.writeAndFlush(frame).addListener(var1);
        }

        public void buildAndSendUdpTo(Channel channel, String host, int port) {
            byte[] bytes = buildPackage();
            DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(bytes),
                    new InetSocketAddress(host, port));
            channel.writeAndFlush(packet);
        }
        public void buildAndSendUdpTo(Channel channel, String host, int port, GenericFutureListener<? extends Future<? super Void>> var1) {
            byte[] bytes = buildPackage();
            DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(bytes),
                    new InetSocketAddress(host, port));
            channel.writeAndFlush(packet).addListener(var1);
        }

        public HBSPackage.Decoder toDecoder() {
            byte[] bytes = buildPackage();
            return HBSPackage.Decoder.withHeader(getHeader()).readPackageBody(bytes);
        }
    }

    public static class Decoder {
        private final byte[] header;
        private byte[] body;
        private final AtomicInteger readOffset = new AtomicInteger(0);

        private Decoder(byte[] header) {
            assert header.length % 2 == 0;
            this.header = header;
        }
        public static Decoder withDefaultHeader() {
            return withHeader(HBSPackage.TCP_HEADER);
        }
        public static Decoder withHeader(byte[] bytes) {
            return new Decoder(bytes);
        }

        public Decoder readPackageBody(byte[] received) {
            this.body = getPackageBody(received);
            return this;
        }

        private byte[] getPackageBody(byte[] received) {
            if (received.length < this.header.length) {
                return new byte[]{};
            }
            byte[] readHeader = new byte[this.header.length];
            System.arraycopy(received, 0, readHeader, 0, this.header.length);
            boolean matchHeader = Arrays.equals(this.header, readHeader);
            if (!matchHeader) {
                throw new RuntimeException("header not match");
            }

            byte[] bodyLenBytes = new byte[4];
            if (received.length < this.header.length + bodyLenBytes.length) {
                return new byte[]{};
            }
            System.arraycopy(received, this.header.length, bodyLenBytes, 0, bodyLenBytes.length);
            int bodyLen = ByteBuffer.wrap(bodyLenBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            if (bodyLen <= 0) return new byte[]{};

            byte[] bodyBytes = new byte[bodyLen];
            if (received.length < this.header.length + bodyLenBytes.length + bodyBytes.length) {
                System.arraycopy(received, this.header.length + bodyLenBytes.length, bodyBytes, 0,
                        received.length - (this.header.length + bodyLenBytes.length));
                return bodyBytes;
            }
            System.arraycopy(received, this.header.length + bodyLenBytes.length, bodyBytes, 0, bodyBytes.length);
            return bodyBytes;
        }

        public Builder toBuilder() {
            // header +(int) bodyLen + body
            return Builder.withHeader(this.header)
                    .writeInt(this.body.length)
                    .writeBytes(this.body);
        }

        /**
         * 将解码出来的数据转换成对象
         * @param t 要转换的对象
         * @param objSetters 对象的setter方法
         * @param <T> 对象类型
         * @return 对象
         */
        public <T> T decode2Obj(T t, Function<T, DecodeField<?>>... objSetters) {
            if (Objects.nonNull(objSetters)) {
                for (Function<T, DecodeField<?>> objSetter : objSetters) {
                    DecodeField<?> decodeField = objSetter.apply(t);
                    Consumer setter = decodeField.getSetter();
                    Class<?> aClass = decodeField.gettClass();
                    if (aClass == String.class) {
                        setter.accept(this.readStr());
                    }
                    if (aClass == Integer.class) {
                        setter.accept(this.readInt());
                    }
                    if (aClass == int.class) {
                        setter.accept(this.readInt());
                    }
                    if (aClass == Long.class) {
                        setter.accept(this.readLong());
                    }
                    if (aClass == long.class) {
                        setter.accept(this.readLong());
                    }
                    if (aClass == Short.class) {
                        setter.accept(this.readShort());
                    }
                    if (aClass == short.class) {
                        setter.accept(this.readShort());
                    }
                    if (aClass == Byte.class) {
                        setter.accept(this.readByte());
                    }
                    if (aClass == byte.class) {
                        setter.accept(this.readByte());
                    }
                    if (aClass == Boolean.class) {
                        setter.accept(this.readByte() == 1);
                    }
                    if (aClass == boolean.class) {
                        setter.accept(this.readByte() == 1);
                    }
                    if (aClass == byte[].class) {
                        setter.accept(this.readBytes());
                    }
                }
            }
            return t;
        }

        /**
         * 重置已读下标
         */
        public Decoder resetBodyReadOffset() {
            readOffset.set(0);
            return this;
        }

        public byte readByte() {
            byte[] bytes = new byte[1];
            System.arraycopy(body, readOffset.getAndAdd(1), bytes, 0, bytes.length);
            return bytes[0];
        }

        public byte[] readBytes() {
            final int len = readInt();
            byte[] bytes = new byte[len];
            System.arraycopy(body, readOffset.getAndAdd(len), bytes, 0, bytes.length);
            return bytes;
        }

        public short readShort() {
            byte[] bytes = new byte[2];
            System.arraycopy(body, readOffset.getAndAdd(2), bytes, 0, bytes.length);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
        }

        public int readInt() {
            byte[] bytes = new byte[4];
            System.arraycopy(body, readOffset.getAndAdd(4), bytes, 0, bytes.length);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        /**
         * 获取消息类型,阅读偏移量不移动
         */
        public int getMsgType() {
            return skipGetInt();
        }

        /**
         * 获取消息类型,阅读偏移量移动4个字节
         */
        public int readMsgType() {
            return readInt();
        }

        public long readLong() {
            byte[] bytes = new byte[8];
            System.arraycopy(body, readOffset.getAndAdd(8), bytes, 0, bytes.length);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
        }

        public String readStr() {
            final byte[] bytes = readBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public <T> T readObj(Class<T> tClass) {
            String json = readStr();
            Gson gson = new Gson();
            return gson.fromJson(json, tClass);
        }

        /**
         * 读取所有剩下的数据
         */
        public byte[] readAllTheRestBodyData() {
            final int offset = readOffset.get();
            int restDataLen = body.length - offset;
            byte[] bytes = new byte[restDataLen];
            System.arraycopy(body, readOffset.getAndAdd(restDataLen), bytes, 0, bytes.length);
            return bytes;
        }

        public byte skipGetByte(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[1], skips);
            return bytes[0];
        }
        public short skipGetShort(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[2], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
        }
        public int skipGetInt(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[4], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        }
        public long skipGetLong(DecodeSkip... skips) {
            byte[] bytes = skipGet(new byte[8], skips);
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
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
