/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.logging;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.MessageList;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.SocketAddress;

/**
 * A {@link ChannelHandler} that logs all events using a logging framework.
 * By default, all events are logged at <tt>DEBUG</tt> level.
 */
@Sharable
public class LoggingHandler extends ChannelDuplexHandler {

    private static final LogLevel DEFAULT_LEVEL = LogLevel.DEBUG;

    private static final String NEWLINE = String.format("%n");

    private static final String[] BYTE2HEX = new String[256];
    private static final String[] HEXPADDING = new String[16];
    private static final String[] BYTEPADDING = new String[16];
    private static final char[] BYTE2CHAR = new char[256];

    static {
        int i;

        // Generate the lookup table for byte-to-hex-dump conversion
        for (i = 0; i < 10; i ++) {
            StringBuilder buf = new StringBuilder(3);
            buf.append(" 0");
            buf.append(i);
            BYTE2HEX[i] = buf.toString();
        }
        for (; i < 16; i ++) {
            StringBuilder buf = new StringBuilder(3);
            buf.append(" 0");
            buf.append((char) ('a' + i - 10));
            BYTE2HEX[i] = buf.toString();
        }
        for (; i < BYTE2HEX.length; i ++) {
            StringBuilder buf = new StringBuilder(3);
            buf.append(' ');
            buf.append(Integer.toHexString(i));
            BYTE2HEX[i] = buf.toString();
        }

        // Generate the lookup table for hex dump paddings
        for (i = 0; i < HEXPADDING.length; i ++) {
            int padding = HEXPADDING.length - i;
            StringBuilder buf = new StringBuilder(padding * 3);
            for (int j = 0; j < padding; j ++) {
                buf.append("   ");
            }
            HEXPADDING[i] = buf.toString();
        }

        // Generate the lookup table for byte dump paddings
        for (i = 0; i < BYTEPADDING.length; i ++) {
            int padding = BYTEPADDING.length - i;
            StringBuilder buf = new StringBuilder(padding);
            for (int j = 0; j < padding; j ++) {
                buf.append(' ');
            }
            BYTEPADDING[i] = buf.toString();
        }

        // Generate the lookup table for byte-to-char conversion
        for (i = 0; i < BYTE2CHAR.length; i ++) {
            if (i <= 0x1f || i >= 0x7f) {
                BYTE2CHAR[i] = '.';
            } else {
                BYTE2CHAR[i] = (char) i;
            }
        }
    }

    protected final InternalLogger logger;
    protected final InternalLogLevel internalLevel;

    private final LogLevel level;

    /**
     * Creates a new instance whose logger name is the fully qualified class
     * name of the instance with hex dump enabled.
     */
    public LoggingHandler() {
        this(DEFAULT_LEVEL);
    }

    /**
     * Creates a new instance whose logger name is the fully qualified class
     * name of the instance.
     *
     * @param level   the log level
     */
    public LoggingHandler(LogLevel level) {
        if (level == null) {
            throw new NullPointerException("level");
        }

        logger = InternalLoggerFactory.getInstance(getClass());
        this.level = level;
        internalLevel = level.toInternalLevel();
    }

    /**
     * Creates a new instance with the specified logger name and with hex dump
     * enabled.
     */
    public LoggingHandler(Class<?> clazz) {
        this(clazz, DEFAULT_LEVEL);
    }

    /**
     * Creates a new instance with the specified logger name.
     *
     * @param level   the log level
     */
    public LoggingHandler(Class<?> clazz, LogLevel level) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (level == null) {
            throw new NullPointerException("level");
        }
        logger = InternalLoggerFactory.getInstance(clazz);
        this.level = level;
        internalLevel = level.toInternalLevel();
    }

    /**
     * Creates a new instance with the specified logger name.
     */
    public LoggingHandler(String name) {
        this(name, DEFAULT_LEVEL);
    }

    /**
     * Creates a new instance with the specified logger name.
     *
     * @param level   the log level
     */
    public LoggingHandler(String name, LogLevel level) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (level == null) {
            throw new NullPointerException("level");
        }
        logger = InternalLoggerFactory.getInstance(name);
        this.level = level;
        internalLevel = level.toInternalLevel();
    }

    /**
     * Returns the {@link LogLevel} that this handler uses to log
     */
    public LogLevel level() {
        return level;
    }

    protected String format(ChannelHandlerContext ctx, String message) {
        String chStr = ctx.channel().toString();
        StringBuilder buf = new StringBuilder(chStr.length() + message.length() + 1);
        buf.append(chStr);
        buf.append(' ');
        buf.append(message);
        return buf.toString();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx)
            throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "REGISTERED"));
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx)
            throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "UNREGISTERED"));
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "ACTIVE"));
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "INACTIVE"));
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
            Throwable cause) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "EXCEPTION: " + cause), cause);
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
            Object evt) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "USER_EVENT: " + evt));
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void bind(ChannelHandlerContext ctx,
            SocketAddress localAddress, ChannelPromise promise) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "BIND(" + localAddress + ')'));
        }
        super.bind(ctx, localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx,
            SocketAddress remoteAddress, SocketAddress localAddress,
            ChannelPromise promise) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "CONNECT(" + remoteAddress + ", " + localAddress + ')'));
        }
        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx,
            ChannelPromise promise) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "DISCONNECT()"));
        }
        super.disconnect(ctx, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx,
            ChannelPromise promise) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "CLOSE()"));
        }
        super.close(ctx, promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx,
             ChannelPromise promise) throws Exception {
        if (logger.isEnabled(internalLevel)) {
            logger.log(internalLevel, format(ctx, "DEREGISTER()"));
        }
        super.deregister(ctx, promise);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageList<Object> msgs) throws Exception {
        logMessages(ctx, "RECEIVED", msgs);
        ctx.fireMessageReceived(msgs);
    }

    @Override
    public void write(ChannelHandlerContext ctx, MessageList <Object> msgs, ChannelPromise promise) throws Exception {
        logMessages(ctx, "WRITE", msgs);
        ctx.write(msgs, promise);
    }

    private void logMessages(ChannelHandlerContext ctx, String message, MessageList<Object> msgs) {
        if (logger.isEnabled(internalLevel)) {
            int size = msgs.size();
            for (int i = 0; i < size; i ++) {
                Object msg = msgs.get(i);
                if (msg instanceof ByteBuf) {
                    logger.log(internalLevel, format(ctx, formatBuffer(message, (ByteBuf) msg)));
                } else {

                }
            }
        }
    }

    protected String formatBuffer(String message, ByteBuf buf) {
        int length = buf.readableBytes();
        int rows = length / 16 + (length % 15 == 0? 0 : 1) + 4;
        StringBuilder dump = new StringBuilder(rows * 80 + message.length() + 16);

        dump.append(message).append('(').append(length).append('B').append(')');
        dump.append(
                NEWLINE + "         +-------------------------------------------------+" +
                        NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" +
                        NEWLINE + "+--------+-------------------------------------------------+----------------+");

        final int startIndex = buf.readerIndex();
        final int endIndex = buf.writerIndex();

        int i;
        for (i = startIndex; i < endIndex; i ++) {
            int relIdx = i - startIndex;
            int relIdxMod16 = relIdx & 15;
            if (relIdxMod16 == 0) {
                dump.append(NEWLINE);
                dump.append(Long.toHexString(relIdx & 0xFFFFFFFFL | 0x100000000L));
                dump.setCharAt(dump.length() - 9, '|');
                dump.append('|');
            }
            dump.append(BYTE2HEX[buf.getUnsignedByte(i)]);
            if (relIdxMod16 == 15) {
                dump.append(" |");
                for (int j = i - 15; j <= i; j ++) {
                    dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
                }
                dump.append('|');
            }
        }

        if ((i - startIndex & 15) != 0) {
            int remainder = length & 15;
            dump.append(HEXPADDING[remainder]);
            dump.append(" |");
            for (int j = i - remainder; j < i; j ++) {
                dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
            }
            dump.append(BYTEPADDING[remainder]);
            dump.append('|');
        }

        dump.append(
                NEWLINE + "+--------+-------------------------------------------------+----------------+");

        return dump.toString();
    }

    protected String formatBuffer(String message, Object[] msgs, int index, int length) {
        return message + '(' + length + "): " + contentToString(msgs, index, length);
    }

    private static String contentToString(Object[] msgs, int index, int length) {
        if (length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = index; i < length; i++) {
            Object msg = msgs[i];
            sb.append(msg);

            if (i + 1 < length) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
    }
}
