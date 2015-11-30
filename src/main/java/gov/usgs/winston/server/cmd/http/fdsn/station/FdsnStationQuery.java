package gov.usgs.winston.server.cmd.http.fdsn.station;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.FdsnException;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnQueryCommand;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnConstraint;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnStationQuery extends FdsnQueryCommand implements FdsnStationService {

    private static final String DEFAULT_LEVEL = "station";
    
    public FdsnStationQuery(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        version = VERSION;
        channels = new Channels(winston);
    }


    protected void sendResponse() {
        if (!arguments.containsKey("level"))
            arguments.put("level", DEFAULT_LEVEL);
        try {
            if ("text".equals(arguments.get("format")))
                sendText();
            else
                sendXml();
        } catch (FdsnException e) {
            sendError(e.code, e.message);
        }
    }

    private void sendText() throws FdsnException {
        if (prunedChanList.length == 0)
            throw new FdsnException(404, "no data");

        String responseText = null;
        String level = arguments.get("level");

        if ("network".equals(level))
            responseText = generateNetworkText();
        else if ("channel".equals(level))
            responseText = generateChannelText();
        else if ("station".equals(level))
            responseText = generateStationText();
        else
            throw new FdsnException(404, "no data");

        HttpResponse response = new HttpResponse("text/plain");
        response.setLength(responseText.length());
        netTools.writeString(response.getHeaderString(), socketChannel);
        netTools.writeString(responseText, socketChannel);
    }

    private String generateNetworkText() {
        StringBuilder out = new StringBuilder();
        out.append("#Network | Description | StartTime | EndTime | TotalStations \n");
        double stationStart = Double.MAX_VALUE;
        double stationEnd = -Double.MAX_VALUE;
        Channel previousChannel = null;
        int stationCount = 0;

        for (Channel c : prunedChanList) {

            if (previousChannel == null)
                previousChannel = c;

            if (!c.network.equals(previousChannel.network)) {
                out.append(previousChannel.network + " |  | " + dateFormat.format(Util.j2KToDate(stationStart)) + " | "
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

        out.append(previousChannel.network + " |  | " + dateFormat.format(Util.j2KToDate(stationStart)) + " | "
                + dateFormat.format(Util.j2KToDate(stationEnd)) + " | " + stationCount + "\n");

        return out.toString();
    }

    private String generateStationText() {
        StringBuilder out = new StringBuilder();
        out.append("#Network | Station | Latitude | Longitude | Elevation | SiteName | StartTime | EndTime\n");
        double stationStart = Double.MAX_VALUE;
        double stationEnd = -Double.MAX_VALUE;
        Channel previousChannel = null;

        for (Channel c : prunedChanList) {

            if (previousChannel == null)
                previousChannel = c;

            if (!c.station.equals(previousChannel.station)) {
                out.append(c.network + "|" + c.station + "|" + c.getInstrument().getLatitude() + "|"
                        + c.getInstrument().getLongitude() + "|" + c.getInstrument().getHeight() + "|"
                        + c.getInstrument().getDescription() + "|" + dateFormat.format(Util.j2KToDate(stationStart))
                        + "|" + dateFormat.format(Util.j2KToDate(stationEnd)) + "\n");
                stationStart = c.getMinTime();
                stationEnd = c.getMaxTime();
            } else {
                stationStart = Math.min(stationStart, c.getMinTime());
                stationEnd = Math.max(stationEnd, c.getMaxTime());
            }

            previousChannel = c;
        }

        out.append(previousChannel.network + "|" + previousChannel.station + "|"
                + previousChannel.getInstrument().getLatitude() + "|" + previousChannel.getInstrument().getLongitude()
                + "|" + previousChannel.getInstrument().getHeight() + "|" + previousChannel.getInstrument().getName()
                + "|" + dateFormat.format(Util.j2KToDate(stationStart)) + "|"
                + dateFormat.format(Util.j2KToDate(stationEnd)) + "\n");
        stationStart = Double.MAX_VALUE;
        stationEnd = -Double.MAX_VALUE;

        return out.toString();
    }

    private String generateChannelText() {
        StringBuilder out = new StringBuilder();
        out.append("#Network | Station | Location | Channel | Latitude | Longitude | Elevation | Depth | Azimuth | Dip | Instrument | Scale | ScaleFreq | ScaleUnits | SampleRate | StartTime | EndTime\n");

        for (Channel c : prunedChanList) {
            String loc = c.location.equals("--") ? "  " : c.location;
            out.append(c.network + "|" + c.station + "|" + loc + "|" + c.channel + "|"
                    + c.getInstrument().getLatitude() + "|" + c.getInstrument().getLongitude() + "|"
                    + c.getInstrument().getHeight() + "||||||||||" + dateFormat.format(Util.j2KToDate(c.getMinTime()))
                    + "|" + dateFormat.format(Util.j2KToDate(c.getMaxTime())) + "\n");
        }

        return out.toString();
    }

    private void sendXml() throws FdsnException {
        if (prunedChanList.length == 0)
            throw new FdsnException(404, "no data");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new FdsnException(404, "no data");
        }

        Document doc = dBuilder.newDocument();
        Element rootElement = createRootElement(doc);

        Element networkElement = null;
        Element stationElement = null;
        double stationStart = Double.MAX_VALUE;
        double stationEnd = -Double.MAX_VALUE;
        String net = null;
        String sta = null;
        String level = arguments.get("level");
        for (Channel c : prunedChanList) {

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
                    String s = dateFormat.format(Util.j2KToDate(stationStart));
                    stationElement.setAttribute("startDate", s);
                    stationStart = Double.MAX_VALUE;

                    String e = dateFormat.format(Util.j2KToDate(stationEnd));
                    stationElement.setAttribute("endDate", e);
                    stationEnd = -Double.MAX_VALUE;

                    Element creationElement = doc.createElement("CreationDate");
                    creationElement.appendChild(doc.createTextNode(dateFormat.format(Util.j2KToDate(stationStart))));
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

        DOMImplementation impl = doc.getImplementation();
        DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
        LSSerializer lsSerializer = implLS.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("format-pretty-print", true);

        LSOutput lsOutput = implLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        Writer stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(doc, lsOutput);
        String responseXml = stringWriter.toString();

        if (responseXml == null || responseXml.length() == 0)
            throw new FdsnException(404, "no data");
        else {

            HttpResponse response = new HttpResponse("application/xml");
            response.setLength(responseXml.length());
            netTools.writeString(response.getHeaderString(), socketChannel);
            netTools.writeString(responseXml, socketChannel);
        }
    }

    private Element createRootElement(Document doc) {
        Element rootElement = doc.createElement("FDSNStationXML");
        rootElement.setAttribute("schemaVersion", "1.0");
        rootElement.setAttribute("xsi:schemaLocation", "http://www.fdsn.org/xml/station/fdsn-station-1.0.xsd");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("xmlns", "http://www.fdsn.org/xml/station/1");
        doc.appendChild(rootElement);

        Element source = doc.createElement("source");
        source.appendChild(doc.createTextNode("Winston"));
        rootElement.appendChild(source);

        Element module = doc.createElement("module");
        module.appendChild(doc.createTextNode("Winston " + WWS.getVersion()));
        rootElement.appendChild(module);

        Element created = doc.createElement("Created");
        created.appendChild(doc.createTextNode(dateFormat.format(new Date(System.currentTimeMillis()))));
        rootElement.appendChild(created);

        return rootElement;
    }
    
    private Element createStationElement(Channel c, Document doc) {
        Element station = doc.createElement("Station");
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
        Element n = doc.createElement("Name");
        n.appendChild(doc.createTextNode(c.getInstrument().getDescription()));
        e.appendChild(n);
        station.appendChild(e);
        
        return station;
    }

    private Element createChannelElement(Channel c, Document doc) {
        Element channelElement = doc.createElement("Channel");
        String loc = c.location.equals("--") ? "  " : c.location;
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


    public String getCommand() {
        return "/fdsnws/station/1/query";
    }

}
