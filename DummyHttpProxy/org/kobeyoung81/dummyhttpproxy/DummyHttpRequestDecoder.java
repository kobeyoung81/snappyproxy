package org.kobeyoung81.dummyhttpproxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;

public class DummyHttpRequestDecoder  extends SimpleChannelInboundHandler<HttpRequest>{

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg)
			throws Exception {
		if (msg instanceof HttpContent) {
			ctx.fireChannelRead(((HttpContent)msg).content());
		}
		else if (msg instanceof DefaultFullHttpRequest) {
			ctx.fireChannelRead(((DefaultFullHttpRequest)msg).content());
		}
		else if (msg instanceof HttpRequest){
			System.out.println("Simple HttpRequest" + msg.toString());
			ctx.fireChannelRead("");
		}
	}
}
