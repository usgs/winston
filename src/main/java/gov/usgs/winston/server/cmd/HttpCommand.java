package gov.usgs.winston.server.cmd;

import gov.usgs.net.CommandHandler;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.MimeType;
import gov.usgs.net.NetTools;
import gov.usgs.winston.Channel;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.AbstractHttpCommand;
import gov.usgs.winston.server.cmd.http.HttpBaseCommand;
import gov.usgs.winston.server.cmd.http.HttpGapsCommand;
import gov.usgs.winston.server.cmd.http.HttpHeliCommand;
import gov.usgs.winston.server.cmd.http.HttpMenuCommand;
import gov.usgs.winston.server.cmd.http.HttpRsamCommand;
import gov.usgs.winston.server.cmd.http.HttpStatusCommand;
import gov.usgs.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectQuery;
import gov.usgs.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectUsage;
import gov.usgs.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectVersion;
import gov.usgs.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectWadl;
import gov.usgs.winston.server.cmd.http.fdsn.station.FdsnStationQuery;
import gov.usgs.winston.server.cmd.http.fdsn.station.FdsnStationUsage;
import gov.usgs.winston.server.cmd.http.fdsn.station.FdsnStationVersion;
import gov.usgs.winston.server.cmd.http.fdsn.station.FdsnStationWadl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * Parse http get request and pass to correct sub-command
 * 
 * @author Dan Cervelli
 */
public class HttpCommand extends BaseCommand {

    private CommandHandler commandHandler;
    private LinkedHashMap<String, AbstractHttpCommand> httpCommands;

    public HttpCommand(NetTools nt, WinstonDatabase db, WWS wws, CommandHandler ch) {
        super(nt, db, wws);
        commandHandler = ch;
        httpCommands = new LinkedHashMap<String, AbstractHttpCommand>();

        addCommand(new HttpMenuCommand(nt, db, wws));
        addCommand(new HttpHeliCommand(nt, db, wws));
        addCommand(new HttpRsamCommand(nt, db, wws));
        addCommand(new HttpStatusCommand(nt, db, wws));
        addCommand(new HttpGapsCommand(nt, db, wws));
        addCommand(new FdsnDataselectQuery(nt, db, wws));
        addCommand(new FdsnDataselectVersion(nt, db, wws));
        addCommand(new FdsnDataselectWadl(nt, db, wws));
        addCommand(new FdsnDataselectUsage(nt, db, wws));
        addCommand(new FdsnStationQuery(nt, db, wws));
        addCommand(new FdsnStationVersion(nt, db, wws));
        addCommand(new FdsnStationWadl(nt, db, wws));
        addCommand(new FdsnStationUsage(nt, db, wws));
    }

    protected void addCommand(AbstractHttpCommand cmd) {
        httpCommands.put(cmd.getCommand(), cmd);
    }

    public void doCommand(Object info, SocketChannel channel) {
        String cmd = (String) info;
        HttpRequest request = new HttpRequest(cmd);

        wws.log(Level.FINER, cmd, channel);

        // AbstractHttpCommand command = httpCommands.get(request.getFile());
        AbstractHttpCommand command = getCommand(request.getFile());
        if (command != null)
            command.respond(cmd, channel, request);
        else {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("www" + request.getFile());
            String mimeType = MimeType.guessMimeType(request.getFile());

            if (!"/".equals(request.getFile()) && in != null)
                sendStream(in, mimeType, channel);
            else
                sendUsage(cmd, channel);
        }
        if (channel.isOpen())
            commandHandler.closeConnection();
    }

    private AbstractHttpCommand getCommand(String file) {
        AbstractHttpCommand cmd = null;

        int prefixLen = 0;
        for (String key : httpCommands.keySet()) {
            if (file.startsWith(key) && key.length() > prefixLen) {
                cmd = httpCommands.get(key);
                prefixLen = key.length();
            }
        }

        return cmd;
    }

    public void sendStream(InputStream in, String mimeType, SocketChannel channel) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());

            byte[] tmp = new byte[in.available()];

            int i = in.read(tmp);
            while (i > 0) {
                out.write(tmp, 0, i);
                i = in.read(tmp);
            }

            byte[] bytes = out.toByteArray();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            HttpResponse response = new HttpResponse(mimeType);
            response.setLength(bytes.length);
            netTools.writeString(response.getHeaderString(), channel);
            netTools.writeByteBuffer(buf, channel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUsage(String command, SocketChannel channel) {
        HttpRequest req = new HttpRequest(command);
        StringBuilder output = new StringBuilder(64000);
        output.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        output.append("<html><head><link href=\"/style.css\" rel=\"stylesheet\" type=\"text/css\">");

        output.append("<script>");

        output.append("var timeZones = [");
        String[] tzs = TimeZone.getAvailableIDs();
        Arrays.sort(tzs);
        for (String tz : tzs)
            output.append("\"" + tz + "\",");
        output.append("];\n");

        output.append("var channels = [");
        Channels channels = new Channels(winston);
        for (Channel chan : channels.getChannels())
            output.append("\"" + chan.getCode().replace('$', '_') + "\",");
        output.append("];\n");

        output.append("function init() {" + "initTabs();" + "initTimeZones();" + "initChannels();" + "buildMenuUrl();"
                + "buildHeliUrl();" + "buildRsamUrl();" + "buildGapsUrl();" + "}");
        output.append("</script>");
        output.append("<title>Winston Wave Server</title></head><body>\n");

        output.append("<div id=\"wrapper\">\n");
        output.append("<div id=\"intro\">I'm a Winston Wave Server. I'm here to service to <A HREF=\"http://volcanoes.usgs.gov/software/swarm\">Swarm</A> and <A HREF=\"http://www.earthwormcentral.org/\">Earthworm's</A> Wave Viewer. I will also provide plots and status info if given a carefully crafted URL. See the tabs below for details.</div><P><BR><P>\n");
        output.append("<div id=\"tabContainer\">\n");

        output.append("<div id=\"tabs\">\n");
        output.append("<ul>\n");
        int i = 1;

        for (AbstractHttpCommand cmd : httpCommands.values()) {
            if (cmd instanceof HttpBaseCommand)
                output.append("<li id=\"tabHeader_" + i++ + "\">" + ((HttpBaseCommand) cmd).getTitle() + "</li>\n");
        }

        output.append("</ul>\n");
        output.append("</div>\n");

        output.append("<div id=\"tabscontent\">\n");
        i = 1;
        for (AbstractHttpCommand cmd : httpCommands.values()) {
            if (!(cmd instanceof HttpBaseCommand))
                continue;

            output.append("<div class=\"tabpage\" id=\"tabpage_" + i++ + "\">");
            output.append("<h2>" + ((HttpBaseCommand) cmd).getTitle() + "</h2>");
            output.append(((HttpBaseCommand) cmd).getUsage(req));
            output.append("</div>");
        }
        output.append("</div>\n");

        output.append("</div>\n");
        output.append("<p><br><p><b>" + WWS.getVersion() + "</b>\n");
        output.append("</div><script src=\"/tabs.js\"></script>\n");
        output.append("</body></html>\n");

        String html = output.toString();
        HttpResponse response = new HttpResponse("text/html; charset=utf-8");
        response.setLength(html.length());
        if ("/".equals(req.getFile()))
            response.setCode("200");
        else {
            response.setCode("404");
            response.setMessage("file not found");
        }
        netTools.writeString(response.getHeaderString(), channel);
        netTools.writeString(html, channel);
    }
}