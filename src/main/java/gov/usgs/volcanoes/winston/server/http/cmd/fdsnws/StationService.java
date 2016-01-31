/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.ChannelConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.FdsnConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.GeographicCircleConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.GeographicConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.GeographicSquareConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.TimeConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.TimeSimpleConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.TimeWindowConstraint;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

/**
 * Implement FDSN-WS station service.
 * 
 * @author Tom Parker
 *
 */
public class StationService extends FdsnwsService {
  private static final String VERSION = "1.1.2";
  private static final String SERVICE = "station";
  private static final String DEFAULT_LEVEL = "station";

  private static final Logger LOGGER = LoggerFactory.getLogger(StationService.class);

  static {
    version = VERSION;
    service = SERVICE;
  }

  /**
   * Fulfill the request.
   * 
   * @param databasePool winston database pool
   * @param ctx handler context
   * @param request the request
   * @throws UtilException when things go wrong
   * @throws FdsnException when FDSN-WS spec violated
   */
  public static void dispatch(WinstonDatabasePool databasePool, ChannelHandlerContext ctx,
      FullHttpRequest request) throws UtilException, FdsnException {
    String method = request.getUri().split("/")[4];
    int queryStringIdx = method.indexOf('?');
    if (queryStringIdx != -1) {
      method = method.substring(0, queryStringIdx);
    }

    switch (method) {
      case "version":
        sendVersion(ctx, request);
        break;
      case "application.wadl":
        sendWadl(ctx, request);
        break;
      case "query":
        sendQueryResponse(databasePool, ctx, request);
        break;
      default:
        ErrorResponse error = new ErrorResponse(ctx);
        error.request(request);
        error.version(VERSION);
        error.status(HttpResponseStatus.BAD_REQUEST);
        error.shortDescription("Bad request");
        error.detailedDescription("Method cannot be parsed.");
        error.sendError();
    }
  }

  private static void sendQueryResponse(WinstonDatabasePool databasePool, ChannelHandlerContext ctx,
      FullHttpRequest request) throws UtilException, FdsnException {
    Map<String, String> arguments = parseRequest(request);
    final String level = StringUtils.stringToString(arguments.get("level"), DEFAULT_LEVEL);

    List<FdsnConstraint> constraints = buildConstraints(arguments);
    LOGGER.debug("got constraints");
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

    if (channels == null) {
      ErrorResponse error = new ErrorResponse(ctx);
      error.request(request);
      error.version(VERSION);
      error.status(HttpResponseStatus.NOT_FOUND);
      error.shortDescription("No data");
      error.detailedDescription("No matching data found.");
      error.sendError();
      return;
    }

    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      final Document doc = dBuilder.newDocument();
      Element rootElement = createRootElement(doc);

      Element networkElement = null;
      Element stationElement = null;
      double stationStart = Double.MAX_VALUE;
      double stationEnd = -Double.MAX_VALUE;
      String net = null;
      String sta = null;

      for (final Channel c : channels) {
        boolean prune = false;
        Iterator<FdsnConstraint> it = constraints.iterator();
        while (!prune && it.hasNext()) {
          if (!it.next().matches(c))
            prune = true;
        }

        if (prune) {
          continue;
        }

        if (!c.network.equals(net)) {
          net = c.network;
          networkElement = doc.createElement("Network");
          networkElement.setAttribute("code", net);
          rootElement.appendChild(networkElement);
        }

        if ("network".equals(level))
          continue;

        if (!c.station.equals(sta)) {
          if (stationElement != null) {
            final String s = FdsnwsDate.toString(J2kSec.asDate(stationStart));
            stationElement.setAttribute("startDate", s);
            stationStart = Double.MAX_VALUE;

            final String e = FdsnwsDate.toString(J2kSec.asDate(stationEnd));
            stationElement.setAttribute("endDate", e);
            stationEnd = -Double.MAX_VALUE;

            final Element creationElement = doc.createElement("CreationDate");
            creationElement
                .appendChild(doc.createTextNode(FdsnwsDate.toString(J2kSec.asDate(stationStart))));
          }

          sta = c.station;
          stationElement = createStationElement(c, doc);
          networkElement.appendChild(stationElement);
        }

        stationStart = Math.min(stationStart, c.getMinTime());
        stationEnd = Math.max(stationEnd, c.getMaxTime());

        if (!"station".equals(level))
          stationElement.appendChild(createChannelElement(c, doc));
      }


      final String responseXml = serialize(doc);
      FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
          HttpResponseStatus.OK, Unpooled.copiedBuffer(responseXml, Charset.forName("UTF-8")));
      response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, responseXml.length());
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8");

      if (HttpHeaders.isKeepAlive(request)) {
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(response);
    } catch (final ParserConfigurationException e) {
      e.printStackTrace();
    }

  }

  private static List<FdsnConstraint> buildConstraints(Map<String, String> arguments)
      throws FdsnException {
    List<FdsnConstraint> constraints = new ArrayList<FdsnConstraint>();
    constraints.add(ChannelConstraint.build(arguments));
    constraints.add(TimeConstraint.build(arguments));
    constraints.add(GeographicConstraint.build(arguments));
    constraints.addAll(ChannelConstraint.buildMulti(arguments));
    
    return constraints;
  }

  private static Element createStationElement(final Channel c, final Document doc) {
    final Element station = doc.createElement("Station");
    station.setAttribute("code", c.station);

    Element e;
    e = doc.createElement("Latitude");
    e.appendChild(doc.createTextNode("" + c.getInstrument().getLatitude()));
    station.appendChild(e);

    e = doc.createElement("Longitude");
    e.appendChild(doc.createTextNode("" + c.getInstrument().getLongitude()));
    station.appendChild(e);

    e = doc.createElement("Elevation");
    e.appendChild(doc.createTextNode("" + c.getInstrument().getHeight()));
    station.appendChild(e);

    e = doc.createElement("Site");
    final Element n = doc.createElement("Name");
    n.appendChild(doc.createTextNode(c.getInstrument().getDescription()));
    e.appendChild(n);
    station.appendChild(e);

    return station;
  }

  private static Element createChannelElement(final Channel c, final Document doc) {
    final Element channelElement = doc.createElement("Channel");
    final String loc = c.location.equals("--") ? "  " : c.location;
    channelElement.setAttribute("locationCode", loc);
    channelElement.setAttribute("code", c.channel);

    Element e;
    e = doc.createElement("Latitude");
    e.appendChild(doc.createTextNode("" + c.getInstrument().getLatitude()));
    channelElement.appendChild(e);

    e = doc.createElement("Longitude");
    e.appendChild(doc.createTextNode("" + c.getInstrument().getLongitude()));
    channelElement.appendChild(e);

    e = doc.createElement("Elevation");
    e.appendChild(doc.createTextNode("" + c.getInstrument().getHeight()));
    channelElement.appendChild(e);

    e = doc.createElement("Depth");
    e.setAttribute("Supported", "no");
    e.appendChild(doc.createTextNode("" + 0));
    channelElement.appendChild(e);

    return channelElement;
  }

  private static Map<String, String> parseRequest(FullHttpRequest request) {
    Map<String, String> arguments = new HashMap<String, String>();


    if (request.getMethod() == HttpMethod.GET) {
      QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());

      for (String name : decoder.parameters().keySet()) {
        arguments.put(name, decoder.parameters().get(name).get(0));
        LOGGER.info("{} : {}", name, decoder.parameters().get(name).get(0));
      }
    } else if (request.getMethod() == HttpMethod.POST) {
      String[] lines = request.content().toString(CharsetUtil.UTF_8).split("\n");
      StringBuffer chans = new StringBuffer();
      for (String list : lines) {
        int idx = list.indexOf('=');
        if (idx != -1) {
          arguments.put(list.substring(0, idx), list.substring(idx, list.length()));
        } else {
          chans.append(list);
        }
      }
      if (chans.length() > 0) {
        arguments.put("chans", chans.toString());
      }
    }

    return arguments;
  }
}
