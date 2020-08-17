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

import java.io.IOException;
import java.net.ServerSocket;

/**
 * This class represents an established socket connection. It's being used by TaskExecutor when
 * SO_REUSEPORT is not required.
 */
final class NonPortReusableConnection implements Connection {
  final ServerSocket serverSocket;

  private NonPortReusableConnection(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
  }

  static NonPortReusableConnection create() {
    // Why do we need a separate connection implementation with ServerSocket for connection
    // where SO_REUSEPORT is not required?
    // - Since PortReusableConnection with Netty's EpollEventLoopGroup only works with
    //   Linux(https://netty.io/4.0/api/io/netty/channel/epoll/EpollEventLoopGroup.html). Having
    //   a separate implementation with ServerSocket when SO_REUSEPORT is not being used enables
    //   tony and e2e unit tests on Mac.
    try {
      ServerSocket serverSocket = new ServerSocket(0);
      return new NonPortReusableConnection(serverSocket);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void close() throws IOException {
    this.serverSocket.close();
  }

  @Override
  public int getPort() {
    return this.serverSocket.getLocalPort();
  }
}
