package gov.usgs.volcanoes.winston.server.cmd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.logging.Level;

import gov.usgs.net.CommandHandler;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.MimeType;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.cmd.http.AbstractHttpCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpGapsCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpHeliCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpMenuCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpRsamCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.HttpStatusCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectQuery;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectUsage;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectVersion;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.dataselect.FdsnDataselectWadl;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.station.FdsnStationQuery;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.station.FdsnStationUsage;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.station.FdsnStationVersion;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.station.FdsnStationWadl;

/**
 * Parse http get request and pass to correct sub-command
 *
 * @author Dan Cervelli
 */
public class HttpCommand extends BaseCommand {

  private final CommandHandler commandHandler;
  private final LinkedHashMap<String, AbstractHttpCommand> httpCommands;

  public HttpCommand(final NetTools nt, final WinstonDatabase db, final WWS wws,
      final CommandHandler ch) {
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

  protected void addCommand(final AbstractHttpCommand cmd) {
    httpCommands.put(cmd.getCommand(), cmd);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final String cmd = (String) info;
    final HttpRequest request = new HttpRequest(cmd);

    wws.log(Level.FINER, cmd, channel);

    // AbstractHttpCommand command = httpCommands.get(request.getFile());
    final AbstractHttpCommand command = getCommand(request.getFile());
    if (command != null)
      command.respond(cmd, channel, request);
    else {
      final InputStream in =
          this.getClass().getClassLoader().getResourceAsStream("www" + request.getFile());
      final String mimeType = MimeType.guessMimeType(request.getFile());

      if (!"/".equals(request.getFile()) && in != null)
        sendStream(in, mimeType, channel);
      else
        sendUsage(cmd, channel);
    }
    if (channel.isOpen())
      commandHandler.closeConnection();
  }

  private AbstractHttpCommand getCommand(final String file) {
    AbstractHttpCommand cmd = null;

    int prefixLen = 0;
    for (final String key : httpCommands.keySet()) {
      if (file.startsWith(key) && key.length() > prefixLen) {
        cmd = httpCommands.get(key);
        prefixLen = key.length();
      }
    }

    return cmd;
  }

  public void sendStream(final InputStream in, final String mimeType, final SocketChannel channel) {
    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());

      final byte[] tmp = new byte[in.available()];

      int i = in.read(tmp);
      while (i > 0) {
        out.write(tmp, 0, i);
        i = in.read(tmp);
      }

      final byte[] bytes = out.toByteArray();
      final ByteBuffer buf = ByteBuffer.wrap(bytes);
      final HttpResponse response = new HttpResponse(mimeType);
      response.setLength(bytes.length);
      netTools.writeString(response.getHeaderString(), channel);
      netTools.writeByteBuffer(buf, channel);
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void sendUsage(final String command, final SocketChannel channel) {
    final HttpRequest req = new HttpRequest(command);
    final StringBuilder output = new StringBuilder(64000);
    output.append(
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
    output.append("<html><head><link href=\"/style.css\" rel=\"stylesheet\" type=\"text/css\">");

    output.append("<script>");

    output.append("var timeZones = [");
    final String[] tzs = TimeZone.getAvailableIDs();
    Arrays.sort(tzs);
    for (final String tz : tzs)
      output.append("\"" + tz + "\",");
    output.append("];\n");

    output.append("var channels = [");
    final Channels channels = new Channels(winston);
    for (final Channel chan : channels.getChannels())
      output.append("\"" + chan.getCode().replace('$', '_') + "\",");
    output.append("];\n");

    output.append("function init() {" + "initTabs();" + "initTimeZones();" + "initChannels();"
        + "buildMenuUrl();" + "buildHeliUrl();" + "buildRsamUrl();" + "buildGapsUrl();" + "}");
    output.append("</script>");
    output.append("<title>Winston Wave Server</title></head><body>\n");

    output.append("<div id=\"wrapper\">\n");
    output.append(
        "<div id=\"intro\">I'm a Winston Wave Server. I'm here to service to <A HREF=\"http://volcanoes.usgs.gov/software/swarm\">Swarm</A> and <A HREF=\"http://www.earthwormcentral.org/\">Earthworm's</A> Wave Viewer. I will also provide plots and status info if given a carefully crafted URL. See the tabs below for details.</div><P><BR><P>\n");
    output.append("<div id=\"tabContainer\">\n");

    output.append("<div id=\"tabs\">\n");
    output.append("<ul>\n");
    int i = 1;

    for (final AbstractHttpCommand cmd : httpCommands.values()) {
      if (cmd instanceof HttpBaseCommand)
        output.append(
            "<li id=\"tabHeader_" + i++ + "\">" + ((HttpBaseCommand) cmd).getTitle() + "</li>\n");
    }

    output.append("</ul>\n");
    output.append("</div>\n");

    output.append("<div id=\"tabscontent\">\n");
    i = 1;
    for (final AbstractHttpCommand cmd : httpCommands.values()) {
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

    final String html = output.toString();
    final HttpResponse response = new HttpResponse("text/html; charset=utf-8");
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
