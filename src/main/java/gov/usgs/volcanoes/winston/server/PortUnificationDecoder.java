package gov.usgs.volcanoes.winston.server;

import java.util.List;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * Classifies requests by protocol and rejiggers pipeline accordingly. 
 * 
 * Assumptions:
 * <ul>
 *  <li>Only WWS and HTTP protocols are used</li>
 *  <li>All commands are at least LEN bytes long</li>
 *  <li>The first LEN bytes of commands are case insensitive</li>
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
      System.out.println((char)bytes[idx]);
    }
    
    if (startsWith(bytes, GET) || startsWith(bytes, POST)) {
      switchToHttp(ctx);
    } else {
      switchToWws(ctx);
    }
  }

  private static boolean startsWith(byte[] array1, byte[] array2) {
    for (int idx = 0; idx < array2.length; idx++) {
      System.out.println(":" + (char)array1[idx] + ":" + (char)array2[idx] + ":");
      if (array1[idx] != array2[idx])
        return false;
    }
    
    return true;
  }
  private void switchToHttp(ChannelHandlerContext ctx) {
    System.out.println("Found HTTP");
    ChannelPipeline p = ctx.pipeline();
    p.addLast("decoder", new HttpRequestDecoder());
    p.addLast("aggregator", new HttpObjectAggregator(1048576));
    p.addLast("encoder", new HttpResponseEncoder());
    p.addLast("handler", new HttpServerHandler());
    p.remove(this);
  }

  private void switchToWws(ChannelHandlerContext ctx) {
    System.out.println("found WWS");

        ChannelPipeline p = ctx.pipeline();
//    p.addLast("decoder", new BigIntegerDecoder());
//    p.addLast("encoder", new NumberEncoder());
//    p.addLast("handler", new WwsServerHandler());
    p.remove(this);
  }
}