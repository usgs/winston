/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.http.HttpConstants;
import gov.usgs.volcanoes.winston.server.http.UnsupportedMethodException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 *
 * @author Tom Parker
 *
 */
public final class MenuCommand extends HttpBaseCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(MenuCommand.class);

  /**
   * Constructor.
   */
  public MenuCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws UtilException, MalformedCommandException {
    StringBuffer error = new StringBuffer();

    Map<String, String> params;
    try {
      params = getUnaryParams(request);
    } catch (UnsupportedMethodException e) {
      // TODO return 501 error
      throw new MalformedCommandException("Unsupported method.");
    }
    // validate input. Write error and return if bad.
    final int sortCol = StringUtils.stringToInt(params.get("ob"), HttpConstants.ORDER_BY);
    if (sortCol < 1 || sortCol > 8) {
      error.append("Error: could not parse ob = " + params.get("ob") + "<br>");
    }

    final String o = StringUtils.stringToString(params.get("so"), HttpConstants.SORT_ORDER);
    final char order = o.charAt(0);

    if (order != 'a' && order != 'd') {
      error.append("Error: could not parse so = " + params.get("so") + "<br>");
    }

    final String tz = StringUtils.stringToString(params.get("tz"), HttpConstants.TIME_ZONE);
    final TimeZone timeZone = TimeZone.getTimeZone(tz);

    if (error.length() > 0) {
      throw new MalformedCommandException(error.toString());
    } else {

      String html = prepareResponse(sortCol, order, timeZone);

      if (html != null) {
        FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
            HttpResponseStatus.OK, Unpooled.copiedBuffer(html, Charset.forName("UTF-8")));
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, html.length());
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");

        if (HttpHeaders.isKeepAlive(request)) {
          response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);
      } else {
        LOGGER.error("NULL server menu.");
      }
    }
  }


  private String prepareResponse(int sortCol, char order, TimeZone timeZone) throws UtilException {
    // write header
    final String[] colTitle = {null, "Pin", "S", "C", "N", "L", "Earliest", "Most Recent", "Type"};
    colTitle[sortCol] += order == 'a' ? " &#9652;" : " &#9662;";

    final char[] colOrd = new char[colTitle.length];
    Arrays.fill(colOrd, 'a');
    if (order == 'a')
      colOrd[sortCol] = 'd';

    final StringBuilder output = new StringBuilder();

    output.append("<HTML><HEAD><TITLE>Winston Server Menu</TITLE></HEAD><BODY>");

    output.append("<table CELLPADDING=\"5\"><tr>");
    for (int i = 1; i < colTitle.length; i++)
      output.append(
          "<th><a href=\"?ob=" + i + "&so=" + colOrd[i] + "\">" + colTitle[i] + "</a></th>");

    output.append("</tr>");

    // get and sort menu
    List<Channel> channels;
    try {
      channels = databasePool.doCommand(new WinstonConsumer<List<Channel>>() {

        public List<Channel> execute(WinstonDatabase winston) throws UtilException {
          return new Channels(winston).getChannels();
        }

      });
    } catch (Exception e) {
      throw new UtilException(e.getMessage());
    }

    final List<String> list =
        gov.usgs.volcanoes.winston.server.wws.cmd.MenuCommand.generateMenu(channels, true);


    final String[][] menu = new String[list.size()][8];
    int i = 0;
    for (final String s : list)
      menu[i++] = s.split("\\s");

    Arrays.sort(menu, getMenuComparator(sortCol, order));

    // display menu items
    for (final String[] line : menu) {
      if (line.length < 8) {
        output.append("can't parse line, skipping. " + Arrays.toString(line));
        continue;
      }

      SimpleDateFormat dateF = new SimpleDateFormat(Time.STANDARD_TIME_FORMAT);
      dateF.setTimeZone(timeZone);

      final double start = Double.parseDouble(line[6]);
      final double end = Double.parseDouble(line[7]);

      output.append("<tr>");
      output.append("<td>" + line[1] + "</td>");
      output.append("<td>" + line[2] + "</td>");
      output.append("<td>" + line[3] + "</td>");
      output.append("<td>" + line[4] + "</td>");
      output.append("<td>" + line[5] + "</td>");
      output.append("<td>" + dateF.format(Ew.asEpoch(start)) + "</td>");
      output.append("<td>" + dateF.format(Ew.asEpoch(end)) + "</td>");
      output.append("<td>" + line[8] + "</td>");
      output.append("</tr>\n");
    }

    output.append("</table>");
    output.append("</BODY></HTML>");
    return output.toString();
  }


  private Comparator<String[]> getMenuComparator(final int sortCol, final char order) {
    return new Comparator<String[]>() {
      public int compare(final String[] e1, final String[] e2) {
        // numeric columns
        if (sortCol == 1 || sortCol == 6 || sortCol == 7) {
          final double d1 = Double.parseDouble(e1[sortCol]);
          final double d2 = Double.parseDouble(e2[sortCol]);

          // Do this the hard way to avoid an int overflow.
          // Yes, this bug was encountered in a running system.
          final double d = d1 - d2;
          if (d == 0)
            return 0;

          final int i = (d < 0 ? -1 : 1);

          if (order == 'a')
            return i;
          else
            return -i;
        }
        // textual columns
        else {
          if (order == 'a')
            return e1[sortCol].compareTo(e2[sortCol]);
          else
            return e2[sortCol].compareTo(e1[sortCol]);
        }
      }
    };
  }
}
