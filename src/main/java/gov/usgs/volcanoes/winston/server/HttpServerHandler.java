/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package gov.usgs.volcanoes.winston.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpCommand;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Derived from HttpSnoopServerHandler
 *
 * @author Tom Parker
 *
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);

  private WinstonDatabasePool winstonDatabasePool;
  private ConfigFile configFile;
  
  public HttpServerHandler(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    this.winstonDatabasePool = winstonDatabasePool;
    this.configFile = configFile;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

    final HttpBaseCommand httpWorker = HttpCommand.get(winstonDatabasePool, req.getUri());
    httpWorker.doCommand(ctx, req);
    // If keep-alive is off, close the connection once the content is fully written.
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
