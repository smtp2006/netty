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
package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MessageList;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;

import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * This handler does all the heavy lifting for you to run a websocket server.
 *
 * It takes care of websocket handshaking as well as processing of control frames (Close, Ping, Pong). Text and Binary
 * data frames are passed to the next handler in the pipeline (implemented by you) for processing.
 *
 * See <tt>io.netty.example.http.websocketx.html5.WebSocketServer</tt> for usage.
 *
 * The implementation of this handler assumes that you just want to run  a websocket server and not process other types
 * HTTP requests (like GET and POST). If you wish to support both HTTP requests and websockets in the one server, refer
 * to the <tt>io.netty.example.http.websocketx.server.WebSocketServer</tt> example.
 *
 * To know once a handshake was done you can intercept the
 * {@link io.netty.channel.ChannelInboundHandler#userEventTriggered(ChannelHandlerContext, Object)} and check if the event was of type
 * {@link ServerHandshakeStateEvent#HANDSHAKE_COMPLETE}.
 */
public class WebSocketServerProtocolHandler extends WebSocketProtocolHandler {

    /**
     * Events that are fired to notify about handshake status
     */
    public enum ServerHandshakeStateEvent {
        /**
         * The Handshake was complete succesful and so the channel was upgraded to websockets
         */
        HANDSHAKE_COMPLETE
    }

    private static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY =
            new AttributeKey<WebSocketServerHandshaker>(WebSocketServerHandshaker.class.getName());

    private final String websocketPath;
    private final String subprotocols;
    private final boolean allowExtensions;

    public WebSocketServerProtocolHandler(String websocketPath) {
        this(websocketPath, null, false);
    }

    public WebSocketServerProtocolHandler(String websocketPath, String subprotocols) {
        this(websocketPath, subprotocols, false);
    }

    public WebSocketServerProtocolHandler(String websocketPath, String subprotocols, boolean allowExtensions) {
        this.websocketPath = websocketPath;
        this.subprotocols = subprotocols;
        this.allowExtensions = allowExtensions;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelPipeline cp = ctx.pipeline();
        if (cp.get(WebSocketServerProtocolHandshakeHandler.class) == null) {
            // Add the WebSocketHandshakeHandler before this one.
            ctx.pipeline().addBefore(ctx.name(), WebSocketServerProtocolHandshakeHandler.class.getName(),
                    new WebSocketServerProtocolHandshakeHandler(websocketPath, subprotocols, allowExtensions));
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, MessageList<Object> out) throws Exception {
        if (frame instanceof CloseWebSocketFrame) {
            WebSocketServerHandshaker handshaker = getHandshaker(ctx);
            frame.retain();
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
            return;
        }
        super.decode(ctx, frame, out);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof WebSocketHandshakeException) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, HttpResponseStatus.BAD_REQUEST, Unpooled.wrappedBuffer(cause.getMessage().getBytes()));
            ctx.channel().write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.close();
        }
    }

    static WebSocketServerHandshaker getHandshaker(ChannelHandlerContext ctx) {
        return ctx.attr(HANDSHAKER_ATTR_KEY).get();
    }

    static void setHandshaker(ChannelHandlerContext ctx, WebSocketServerHandshaker handshaker) {
        ctx.attr(HANDSHAKER_ATTR_KEY).set(handshaker);
    }

    static ChannelHandler forbiddenHttpRequestResponder() {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void messageReceived(final ChannelHandlerContext ctx, MessageList<Object> msgs) throws Exception {
                for (int i = 0; i < msgs.size(); i++) {
                    Object msg = msgs.get(i);
                    if (msg instanceof FullHttpRequest) {
                        FullHttpResponse response =
                                new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FORBIDDEN);
                        ctx.channel().write(response);
                        msgs.remove(i--);
                    }
                }
                ctx.fireMessageReceived(msgs);
            }
        };
    }
}
