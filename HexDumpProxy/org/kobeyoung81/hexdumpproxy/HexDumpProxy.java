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
package org.kobeyoung81.hexdumpproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HexDumpProxy {

	static int LOCAL_PORT = 8080;
	static String REMOTE_HOST = "23.226.229.182";
	static int REMOTE_PORT = 1080;
	static String ROLE = "local";

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Too less arguments! " + args.length
					+ " (Role LocalPort RemoteHost RemotePort)");
			return;
		}

		ROLE = args[0];
		LOCAL_PORT = Integer.parseInt(args[1]);
		REMOTE_HOST = args[2];
		REMOTE_PORT = Integer.parseInt(args[3]);

		System.err.println("Proxying *:" + LOCAL_PORT + " to " + REMOTE_HOST
				+ ':' + REMOTE_PORT + " ...");

		// Configure the bootstrap.
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(
							new HexDumpProxyInitializer(ROLE, REMOTE_HOST,
									REMOTE_PORT))
					.childOption(ChannelOption.AUTO_READ, false).bind(LOCAL_PORT)
					.sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
