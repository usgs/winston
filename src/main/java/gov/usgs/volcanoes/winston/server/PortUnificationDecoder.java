/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.server.http.HttpCommandHandler;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandHandler;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandStringDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * Classifies requests by protocol and rejiggers the pipeline accordingly.
 *
 * Assumptions:
 * <ul>
 * <li>Only WWS and HTTP protocols are used</li>
 * <li>The first LEN bytes of commands are case insensitive</li>
 * <li>All command strings are at least LEN bytes long</li>
 * </ul>
 *
 * Derived from the Netty PortUnification example found at:
 * https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/portunification
 * /PortUnificationServerHandler.java
 *
 * @author Tom Parker
 *
 */
public class PortUnificationDecoder extends ByteToMessageDecoder {
  private static final Logger LOGGER = LoggerFactory.getLogger(PortUnificationDecoder.class);
  private static final boolean DEFAULT_ALLOW_HTTP = false;
  private static final int LEN = 5;

  private static final byte[] GET = {'G', 'E', 'T', 0};
  private static final byte[] POST = {'P', 'O', 'S', 'T', 0};

  private static boolean startsWith(byte[] array1, byte[] array2) {
    for (int idx = 0; idx < array2.length; idx++) {
      if (array1[idx] != array2[idx]) {
        return false;
      }
    }

    return true;
  }

  private final ConfigFile configFile;
  private final WinstonDatabasePool winstonDatabasePool;


  /**
   * Constructor.
   * 
   * @param configFile config file
   * @param winstonDatabasePool database pool
   */
  public PortUnificationDecoder(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    super();
    this.configFile = configFile;
    this.winstonDatabasePool = winstonDatabasePool;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    // Will use the first five bytes to detect a protocol.
    if (in.readableBytes() < LEN) {
      return;
    }

    final byte[] bytes = new byte[LEN];
    in.getBytes(0, bytes, 0, LEN);
    for (int idx = 0; idx < LEN; idx++) {
      bytes[idx] = (byte) (bytes[idx] & ~('a' - 'A'));
    }

    final ChannelPipeline p = ctx.pipeline();
    if (startsWith(bytes, GET) || startsWith(bytes, POST)) {
      LOGGER.debug("Detected HTTP connection.");
      if (configFile.getBoolean("wws.allowHttp", DEFAULT_ALLOW_HTTP)) {
        switchToHttp(p);
      } else {
        LOGGER.info("Ignoring HTTP request. (wws.allowHttp = false");
      }
    } else {
      LOGGER.debug("Detected WWS connection");
      switchToWws(p);
    }
  }

  private void switchToHttp(ChannelPipeline pipeline) {
    pipeline.addLast(new HttpRequestDecoder());
    pipeline.addLast(new HttpObjectAggregator(1048576));
    pipeline.addLast(new HttpResponseEncoder());
    pipeline.addLast(new HttpCommandHandler(configFile, winstonDatabasePool));
    pipeline.remove(this);
  }

  private void switchToWws(ChannelPipeline pipeline) {
    pipeline.addLast(new LineBasedFrameDecoder(1024, true, true));
    pipeline.addLast(new StringDecoder(CharsetUtil.US_ASCII));
    pipeline.addLast(new StringEncoder(CharsetUtil.US_ASCII));
    pipeline.addLast(new ByteArrayEncoder());
    pipeline.addLast(new WwsCommandStringDecoder());
    pipeline.addLast(new WwsCommandHandler(configFile, winstonDatabasePool));
    pipeline.remove(this);
  }
  
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (!(cause instanceof java.io.IOException)) {
      try {
        LOGGER.error("Exception caught in PortUnificationDecoder while servicing {}: {} ({})",
            ctx.channel().remoteAddress(), cause.getClass().getName(), cause.getMessage());
      } catch (Exception e) {
        LOGGER.error("Exception caught catching exception. ({})", e.getLocalizedMessage());
      }
    }
    ctx.close();
  }

}
