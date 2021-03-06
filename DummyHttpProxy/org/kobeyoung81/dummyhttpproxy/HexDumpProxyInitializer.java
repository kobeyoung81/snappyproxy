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
package org.kobeyoung81.dummyhttpproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.SnappyFramedDecoder;
import io.netty.handler.codec.compression.SnappyFramedEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HexDumpProxyInitializer extends ChannelInitializer<SocketChannel> {

	private final String remoteHost;
	private final int remotePort;
	private final String role;

	public HexDumpProxyInitializer(String role, String remoteHost,
			int remotePort) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.role = role;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		if ("local".equals(role)) {
			ch.pipeline().addLast(
					new LoggingHandler(LogLevel.ERROR),
					new HexDumpProxyFrontendHandler(role, remoteHost,
							remotePort));
		} else {
			ch.pipeline().addLast(
					new LoggingHandler(LogLevel.ERROR),
					new DummyHttpRequestDecoder(),
					new SnappyFramedDecoder(),
					new HexDumpProxyFrontendHandler(role, remoteHost,
							remotePort), 
					new SnappyFramedEncoder(),
					new DummyHttpResponseEncoder());
		}
	}
}
