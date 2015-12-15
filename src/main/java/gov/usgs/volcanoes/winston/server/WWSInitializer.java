/*
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.server.handler.EchoServerHandler;
import gov.usgs.volcanoes.winston.server.handler.HttpRequestHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

/**
 * Inializer for connection to eBAM.
 * 
 * @author Tom Parker
 *
 */
public class WWSInitializer extends ChannelInitializer<SocketChannel> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WWSInitializer.class);
  
  private static final int READ_TIMEOUT = 30;

  private final ConfigFile configFile;

  /**
   * Constructor.
   * 
   * @param logger My datalogger
   * @param dataFile The data file to poll
   * @param recordIndex The most recent record on disk, or -1 if none are found.
   */
  public WWSInitializer(final ConfigFile configFile) {
    super();
    this.configFile = configFile;
  }

  @Override
  public void initChannel(final SocketChannel chan) throws IOException {
    final ChannelPipeline pipeline = chan.pipeline();

    // Decoders
    pipeline.addLast(new PortUnificationDecoder(configFile));
//    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(READ_TIMEOUT));
//    pipeline.addLast(new HttpRequestDecoder());
//    pipeline.addLast(new HttpRequestHandler("/ws"));
//    pipeline.addLast(new LineBasedFrameDecoder(1024, true, true));
//    pipeline.addLast(new StringDecoder(CharsetUtil.US_ASCII));
//    pipeline.addLast(new EchoServerHandler());


    // Encoders
    pipeline.addLast(new StringEncoder());
  }
}
