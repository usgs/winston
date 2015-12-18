/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.util.List;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.channel.*;
/**
 * Classifies requests by protocol and rejiggers pipeline accordingly.
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

  private static final byte[] GET = {'G', 'E', 'T', 0};

  private static final int LEN = 5;
  private static final Logger LOGGER = LoggerFactory.getLogger(PortUnificationDecoder.class);
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

private ConnectionStatistics connectionStatistics;
private DatabaseStatistics databaseStatistics;

private static final AttributeKey<ConnectionStatistics> connectionStatsKey;
private static final AttributeKey<DatabaseStatistics> databaseStatsKey;

static {
  connectionStatsKey = AttributeKey.valueOf("connectionStatistics");
  databaseStatsKey = AttributeKey.valueOf("databaseStatistics");
}

  /**
   * Constructor.
   *
   * @param configFile Our configFile
   */
  public PortUnificationDecoder(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    super();
    this.configFile = configFile;
    this.winstonDatabasePool = winstonDatabasePool;    
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    connectionStatistics = ctx.channel().attr(connectionStatsKey).get();
    
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
      switchToHttp(p);
    } else {
      switchToWws(p);
    }
  }

  private void switchToHttp(ChannelPipeline p) {
    LOGGER.info("Found HTTP command.");
    connectionStatistics.incrHttpCount();
    
    p.addLast(new HttpRequestDecoder());
    p.addLast(new HttpObjectAggregator(1048576));
    p.addLast(new StringEncoder(CharsetUtil.US_ASCII));
    p.addLast(new HttpResponseEncoder());
    p.addLast(new HttpServerHandler(configFile, winstonDatabasePool));
    p.remove(this);
  }

  private void switchToWws(ChannelPipeline p) {
    LOGGER.info("found WWS command.");
    connectionStatistics.incrWwsCount();

    p.addLast(new LineBasedFrameDecoder(1024, true, true));
    p.addLast(new StringDecoder(CharsetUtil.US_ASCII));
    p.addLast(new StringEncoder(CharsetUtil.US_ASCII));
    p.addLast(new WwsCommandStringDecoder());
    p.addLast(new WwsServerHandler(configFile, winstonDatabasePool));
    p.remove(this);
  }
}