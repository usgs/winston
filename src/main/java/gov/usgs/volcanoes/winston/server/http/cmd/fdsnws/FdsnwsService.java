/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.http.HttpTemplateConfiguration;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.ChannelConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.FdsnConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.GeographicConstraint;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.TimeConstraint;
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
import io.netty.util.CharsetUtil;

/**
 * Abstract FDSN WS class.
 * 
 * @author Tom Parker
 *
 */
abstract public class FdsnwsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FdsnwsService.class);

  protected static String version;
  protected static String service;

  protected static void sendVersion(ChannelHandlerContext ctx, FullHttpRequest request) {
    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
        HttpResponseStatus.OK, Unpooled.copiedBuffer(version, Charset.forName("UTF-8")));
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, version.length());
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (HttpHeaders.isKeepAlive(request)) {
      response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }
    ctx.writeAndFlush(response);
  }

  protected static void sendWadl(ChannelHandlerContext ctx, FullHttpRequest request) {
    Map<String, Object> root = new HashMap<String, Object>();
    root.put("host", ctx.channel().localAddress().toString().substring(1));


    try {
      HttpTemplateConfiguration cfg = HttpTemplateConfiguration.getInstance();
      Template template = cfg.getTemplate("fdsnws/" + service + "_application.ftl");

      Writer sw = new StringWriter();
      template.process(root, sw);
      String xml = sw.toString();
      sw.close();

      FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(),
          HttpResponseStatus.OK, Unpooled.copiedBuffer(xml, Charset.forName("UTF-8")));
      response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, xml.length());
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/xml; charset=UTF-8");

      if (HttpHeaders.isKeepAlive(request)) {
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
      }
      ctx.writeAndFlush(response);
    } catch (IOException e) {
      LOGGER.error(e.getLocalizedMessage());
    } catch (TemplateException e) {
      LOGGER.error(e.getLocalizedMessage());
    }
  }


  protected static Element createRootElement(final Document doc) {
    final Element rootElement = doc.createElement("FDSNStationXML");
    rootElement.setAttribute("schemaVersion", "1.0");
    rootElement.setAttribute("xsi:schemaLocation",
        "http://www.fdsn.org/xml/station/fdsn-station-1.0.xsd");
    rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    rootElement.setAttribute("xmlns", "http://www.fdsn.org/xml/station/1");
    doc.appendChild(rootElement);

    final Element source = doc.createElement("source");
    source.appendChild(doc.createTextNode("Winston"));
    rootElement.appendChild(source);

    final Element module = doc.createElement("module");
    module.appendChild(doc.createTextNode("Winston " + Version.VERSION_STRING));
    rootElement.appendChild(module);

    final Element created = doc.createElement("Created");
    created.appendChild(doc.createTextNode(FdsnwsDate.toString(new Date())));
    rootElement.appendChild(created);
    
    return rootElement;
  }

  protected static String serialize(final Document doc) {
    final DOMImplementation impl = doc.getImplementation();
    final DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
    final LSSerializer lsSerializer = implLS.createLSSerializer();
    lsSerializer.getDomConfig().setParameter("format-pretty-print", true);

    final LSOutput lsOutput = implLS.createLSOutput();
    lsOutput.setEncoding("UTF-8");
    final Writer stringWriter = new StringWriter();
    lsOutput.setCharacterStream(stringWriter);
    lsSerializer.write(doc, lsOutput);
    return stringWriter.toString();
  }
  
  protected static String getArg(Map<String, String> arguments, final String s1, final String s2) {
    String arg = arguments.get(s1);
    if (arg == null)
      arg = arguments.get(s2);

    return arg;
  }
  
  protected static Map<String, String> parseRequest(FullHttpRequest request) {
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


  protected static List<Channel> getChannels(WinstonDatabasePool databasePool)
      throws UtilException {
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
    return channels;
  }



}
