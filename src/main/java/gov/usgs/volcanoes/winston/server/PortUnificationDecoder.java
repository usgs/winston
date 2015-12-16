package gov.usgs.volcanoes.winston.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * Classifies requests by protocol and rejiggers pipeline accordingly.
 * 
 * Assumptions:
 * <ul>
 * <li>Only WWS and HTTP protocols are used</li>
 * <li>All commands are at least LEN bytes long</li>
 * <li>The first LEN bytes of commands are case insensitive</li>
 * </ul>
 * 
 * Derived from the Netty PortUnification example at:
 * https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/portunification
 * /PortUnificationServerHandler.java
 * 
 * @author Tom Parker
 *
 */
public class PortUnificationDecoder extends ByteToMessageDecoder {

  private static final Logger LOGGER = LoggerFactory.getLogger(PortUnificationDecoder.class);
  
  private static final int LEN = 5;
  private static final byte[] GET = {'G', 'E', 'T', 0};
  private static final byte[] POST = {'P', 'O', 'S', 'T', 0};

  private ConfigFile configFile;

  /**
   * Constructor.
   * 
   * @param configFile Our configFile
   */
  public PortUnificationDecoder(ConfigFile configFile) {
    super();
    this.configFile = configFile;
  }


  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    // Will use the first five bytes to detect a protocol.
    if (in.readableBytes() < LEN) {
      return;
    }

    byte[] bytes = new byte[LEN];
    in.getBytes(0, bytes, 0, LEN);
    for (int idx = 0; idx < LEN; idx++) {
      bytes[idx] = (byte) (bytes[idx] & ~('a' - 'A'));
      System.out.println((char) bytes[idx]);
    }

    if (startsWith(bytes, GET) || startsWith(bytes, POST)) {
      switchToHttp(ctx);
    } else {
      switchToWws(ctx);
    }
  }

  private static boolean startsWith(byte[] array1, byte[] array2) {
    for (int idx = 0; idx < array2.length; idx++) {
      if (array1[idx] != array2[idx])
        return false;
    }

    return true;
  }

  private void switchToHttp(ChannelHandlerContext ctx) {
    LOGGER.info("Found HTTP command.");
    
    ChannelPipeline p = ctx.pipeline();
    p.addLast(new HttpRequestDecoder());
    p.addLast(new HttpObjectAggregator(1048576));
    p.addLast(new HttpResponseEncoder());
    p.addLast(new HttpServerHandler());
    p.remove(this);
  }

  private void switchToWws(ChannelHandlerContext ctx) {
    LOGGER.info("found WWS command.");

    ChannelPipeline p = ctx.pipeline();
    p.addLast(new LineBasedFrameDecoder(1024, true, true));
    p.addLast(new StringDecoder(CharsetUtil.US_ASCII));
    p.addLast(new WwsCommandStringDecoder());
    p.addLast(new WwsServerHandler(configFile));
    p.addLast(new StringEncoder(CharsetUtil.US_ASCII));
    p.remove(this);
  }
}
