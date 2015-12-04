package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.station;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnException;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command.FdsnQueryCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnStationQuery extends FdsnQueryCommand implements FdsnStationService {

  private static final String DEFAULT_LEVEL = "station";

  public FdsnStationQuery(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    version = VERSION;
    channels = new Channels(winston);
  }


  @Override
  protected void sendResponse() {
    if (!arguments.containsKey("level"))
      arguments.put("level", DEFAULT_LEVEL);
    try {
      if ("text".equals(arguments.get("format")))
        sendText();
      else
        sendXml();
    } catch (final FdsnException e) {
      sendError(e.code, e.message);
    }
  }

  private void sendText() throws FdsnException {
    if (prunedChanList.length == 0)
      throw new FdsnException(404, "no data");

    String responseText = null;
    final String level = arguments.get("level");

    if ("network".equals(level))
      responseText = generateNetworkText();
    else if ("channel".equals(level))
      responseText = generateChannelText();
    else if ("station".equals(level))
      responseText = generateStationText();
    else
      throw new FdsnException(404, "no data");

    final HttpResponse response = new HttpResponse("text/plain");
    response.setLength(responseText.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(responseText, socketChannel);
  }

  private String generateNetworkText() {
    final StringBuilder out = new StringBuilder();
    out.append("#Network | Description | StartTime | EndTime | TotalStations \n");
    double stationStart = Double.MAX_VALUE;
    double stationEnd = -Double.MAX_VALUE;
    Channel previousChannel = null;
    int stationCount = 0;

    for (final Channel c : prunedChanList) {

      if (previousChannel == null)
        previousChannel = c;

      if (!c.network.equals(previousChannel.network)) {
        out.append(previousChannel.network + " |  | "
            + dateFormat.format(Util.j2KToDate(stationStart)) + " | "
            + dateFormat.format(Util.j2KToDate(stationEnd)) + " | " + stationCount + "\n");
        stationStart = c.getMinTime();
        stationEnd = c.getMaxTime();
        stationCount = 0;
      } else {
        stationCount++;
        stationStart = Math.min(stationStart, c.getMinTime());
        stationEnd = Math.max(stationEnd, c.getMaxTime());
      }

      previousChannel = c;
    }

    out.append(previousChannel.network + " |  | " + dateFormat.format(Util.j2KToDate(stationStart))
        + " | " + dateFormat.format(Util.j2KToDate(stationEnd)) + " | " + stationCount + "\n");

    return out.toString();
  }

  private String generateStationText() {
    final StringBuilder out = new StringBuilder();
    out.append(
        "#Network | Station | Latitude | Longitude | Elevation | SiteName | StartTime | EndTime\n");
    double stationStart = Double.MAX_VALUE;
    double stationEnd = -Double.MAX_VALUE;
    Channel previousChannel = null;

    for (final Channel c : prunedChanList) {

      if (previousChannel == null)
        previousChannel = c;

      if (!c.station.equals(previousChannel.station)) {
        out.append(c.network + "|" + c.station + "|" + c.getInstrument().getLatitude() + "|"
            + c.getInstrument().getLongitude() + "|" + c.getInstrument().getHeight() + "|"
            + c.getInstrument().getDescription() + "|"
            + dateFormat.format(Util.j2KToDate(stationStart)) + "|"
            + dateFormat.format(Util.j2KToDate(stationEnd)) + "\n");
        stationStart = c.getMinTime();
        stationEnd = c.getMaxTime();
      } else {
        stationStart = Math.min(stationStart, c.getMinTime());
        stationEnd = Math.max(stationEnd, c.getMaxTime());
      }

      previousChannel = c;
    }

    out.append(previousChannel.network + "|" + previousChannel.station + "|"
        + previousChannel.getInstrument().getLatitude() + "|"
        + previousChannel.getInstrument().getLongitude() + "|"
        + previousChannel.getInstrument().getHeight() + "|"
        + previousChannel.getInstrument().getName() + "|"
        + dateFormat.format(Util.j2KToDate(stationStart)) + "|"
        + dateFormat.format(Util.j2KToDate(stationEnd)) + "\n");
    stationStart = Double.MAX_VALUE;
    stationEnd = -Double.MAX_VALUE;

    return out.toString();
  }

  private String generateChannelText() {
    final StringBuilder out = new StringBuilder();
    out.append(
        "#Network | Station | Location | Channel | Latitude | Longitude | Elevation | Depth | Azimuth | Dip | Instrument | Scale | ScaleFreq | ScaleUnits | SampleRate | StartTime | EndTime\n");

    for (final Channel c : prunedChanList) {
      final String loc = c.location.equals("--") ? "  " : c.location;
      out.append(c.network + "|" + c.station + "|" + loc + "|" + c.channel + "|"
          + c.getInstrument().getLatitude() + "|" + c.getInstrument().getLongitude() + "|"
          + c.getInstrument().getHeight() + "||||||||||"
          + dateFormat.format(Util.j2KToDate(c.getMinTime())) + "|"
          + dateFormat.format(Util.j2KToDate(c.getMaxTime())) + "\n");
    }

    return out.toString();
  }

  private void sendXml() throws FdsnException {
    if (prunedChanList.length == 0)
      throw new FdsnException(404, "no data");

    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
    } catch (final ParserConfigurationException e) {
      throw new FdsnException(404, "no data");
    }

    final Document doc = dBuilder.newDocument();
    final Element rootElement = createRootElement(doc);

    Element networkElement = null;
    Element stationElement = null;
    double stationStart = Double.MAX_VALUE;
    double stationEnd = -Double.MAX_VALUE;
    String net = null;
    String sta = null;
    final String level = arguments.get("level");
    for (final Channel c : prunedChanList) {

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
          final String s = dateFormat.format(Util.j2KToDate(stationStart));
          stationElement.setAttribute("startDate", s);
          stationStart = Double.MAX_VALUE;

          final String e = dateFormat.format(Util.j2KToDate(stationEnd));
          stationElement.setAttribute("endDate", e);
          stationEnd = -Double.MAX_VALUE;

          final Element creationElement = doc.createElement("CreationDate");
          creationElement
              .appendChild(doc.createTextNode(dateFormat.format(Util.j2KToDate(stationStart))));
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

    final DOMImplementation impl = doc.getImplementation();
    final DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
    final LSSerializer lsSerializer = implLS.createLSSerializer();
    lsSerializer.getDomConfig().setParameter("format-pretty-print", true);

    final LSOutput lsOutput = implLS.createLSOutput();
    lsOutput.setEncoding("UTF-8");
    final Writer stringWriter = new StringWriter();
    lsOutput.setCharacterStream(stringWriter);
    lsSerializer.write(doc, lsOutput);
    final String responseXml = stringWriter.toString();

    if (responseXml == null || responseXml.length() == 0)
      throw new FdsnException(404, "no data");
    else {

      final HttpResponse response = new HttpResponse("application/xml");
      response.setLength(responseXml.length());
      netTools.writeString(response.getHeaderString(), socketChannel);
      netTools.writeString(responseXml, socketChannel);
    }
  }

  private Element createRootElement(final Document doc) {
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
    created
        .appendChild(doc.createTextNode(dateFormat.format(new Date(System.currentTimeMillis()))));
    rootElement.appendChild(created);

    return rootElement;
  }

  private Element createStationElement(final Channel c, final Document doc) {
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

  private Element createChannelElement(final Channel c, final Document doc) {
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


  @Override
  public String getCommand() {
    return "/fdsnws/station/1/query";
  }

}
