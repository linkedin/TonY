/*
 * Copyright 2020 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.linkedin.tony;

import com.google.common.annotations.VisibleForTesting;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.apache.commons.lang3.Range;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class encapsulates netty objects related to an established connection which
 * allows SO_REUSEPORT. It only works with Linux platform since EpollEventLoopGroup used
 * in {@link PortReusableConnection#create(int)} is not supported via other platforms. See
 * <a href="https://netty.io/4.0/api/io/netty/channel/epoll/EpollEventLoopGroup.html">
 *   https://netty.io/4.0/api/io/netty/channel/epoll/EpollEventLoopGroup.html</a>.
 */
final class PortReusableConnection implements Connection {
  private static final Log LOG = LogFactory.getLog(PortReusableConnection.class);
  final EventLoopGroup eventLoopGroup;
  final ChannelFuture future;
  PortReusableConnection(EventLoopGroup loopGroup, ChannelFuture future) {
    this.eventLoopGroup = loopGroup;
    this.future = future;
  }

  /**
   * Closes the netty connection
   */
  @Override
  public void close() {
    if (this.future.channel().isOpen()) {
      this.future.channel().close().awaitUninterruptibly();
    }

    if (!this.eventLoopGroup.isShutdown()) {
      this.eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }
  }

  /**
   * @return the binding port associated with the connection
   */
  @Override
  public int getPort() {
    InetSocketAddress socketAddress =
        (InetSocketAddress) this.future.channel().localAddress();
    return socketAddress.getPort();
  }

  private static boolean isPortAvailable(int port) throws IOException {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (serverSocket != null) {
        serverSocket.close();
      }
    }
  }

  /**
   * Creates connection binding to an available port with SO_REUSEPORT
   *  See <a href="https://lwn.net/Articles/542629/">https://lwn.net/Articles/542629/</a>
   *  about SO_REUSEPORT.
   * @return the created connection
   */
  static PortReusableConnection create() {
    // Why not using port 0 to obtain the port?
    // - Since another tony executor can bind to the same port when SO_REUSEPORT is used.
    //   See how a port is selected by kernel based on a free-list and socket options:
    //   https://idea.popcount.org/2014-04-03-bind-before-connect/#port-allocation .
    final Range<Integer> portRange = Range.between(1024, 65535);
    PortReusableConnection nettyConnection = null;
    for (int port = portRange.getMinimum(); port <= portRange.getMaximum(); port++) {
      try {
        if (isPortAvailable(port)) {
          nettyConnection = create(port);
          break;
        }
      } catch (Exception exception) {
        LOG.debug("Fail to create connection to port " + port);
      }
    }
    return nettyConnection;
  }

  /**
   * Creates connection with netty library which has built-in port reuse support.
   * <p>port reuse feature is detailed in:
   * <a href="https://lwn.net/Articles/542629/">https://lwn.net/Articles/542629/</a>
   * </p>
   *
   * @param port the port to bind to, 0 to bind to a random port
   * @return the created connection
   * @throws BindException if fails to bind to any port
   * @throws InterruptedException if the thread waiting for incoming connection is interrupted
   */
  @VisibleForTesting
  static PortReusableConnection create(int port) throws InterruptedException,
      BindException {
    // Why creating connection with port reuse using netty instead of native socket library
    //(https://docs.oracle.com/javase/8/docs/api/java/net/Socket.html)?
    // - Tony's default Java version is 8 and port reuse feature is only available in Java 9+:
    // https://docs.oracle.com/javase/9/docs/api/java/net/StandardSocketOptions.html#SO_REUSEPORT.
    //
    // Why not upgrading Tony to Java 9+ given port reuse is supported in Java 9+?
    // - In Linkedin, only Java 8 and 11 are officially supported, but Java 11 introduces
    //   incompatibility with Play version tony-portal(https://github.com/linkedin/TonY/tree/master/tony-portal)
    //   is using. Upgrading Play to a Java 11-compatible version requires non-trivial amount of
    //   effort.

    final EventLoopGroup bossGroup = new EpollEventLoopGroup();
    ServerBootstrap b = new ServerBootstrap();

    b.group(bossGroup)
        .channel(EpollServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) {
          }
        }).option(EpollChannelOption.SO_REUSEPORT, true)
        .option(ChannelOption.SO_KEEPALIVE, true);

    ChannelFuture future = b.bind(port).await();
    if (!future.isSuccess()) {
      throw new BindException("Fail to bind to any port");
    }
    return new PortReusableConnection(bossGroup, future);
  }
}

