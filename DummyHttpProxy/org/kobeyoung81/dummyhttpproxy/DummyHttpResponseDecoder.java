package org.kobeyoung81.dummyhttpproxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;

public class DummyHttpResponseDecoder extends SimpleChannelInboundHandler<HttpResponse>{

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg)
			throws Exception {
		if (msg instanceof HttpContent) {
			ctx.fireChannelRead(((HttpContent)msg).content());
		}
		else if (msg instanceof DefaultFullHttpResponse) {
			ctx.fireChannelRead(((DefaultFullHttpResponse)msg).content());
		}
		else if (msg instanceof HttpResponse){
			System.out.println("Simple HttpResponse" + msg.toString());
			ctx.fireChannelRead("");
		}
	}
}
