package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 *
 * @author Tom Parker
 *
 */
public abstract class FdsnUsageCommand extends FdsnCommand {

  protected String UrlBuillderTemplate;
  protected String InterfaceDescriptionTemplate;

  public FdsnUsageCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {
    final HttpRequest req = new HttpRequest(cmd);
    final StringBuilder output = new StringBuilder(64000);
    output.append(
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
    output.append("<html><head><link href=\"/style.css\" rel=\"stylesheet\" type=\"text/css\">");
    output.append("<link href=\"/fdsnws/fdsnws.css\" rel=\"stylesheet\" type=\"text/css\">");
    output.append("<script>");
    output.append("function init() {" + "initTabs();" + "buildUrl();" + "}");
    output.append("</script>");
    output.append("<title>Winston Wave Server</title></head><body>\n");

    output.append("<div id=\"wrapper\">\n");
    output.append(
        "<div id=\"intro\">I'm a Winston Wave Server. I'm here to service to <A HREF=\"http://volcanoes.usgs.gov/software/swarm\">Swarm</A> and <A HREF=\"http://www.earthwormcentral.org/\">Earthworm's</A> Wave Viewer. I will also provide plots and status info if given a carefully crafted URL. Winston specific usage is described on my <a href=\"http://"
            + req.getHeader("Host")
            + "/\">base page</a>. I support a subset of the <a href=\"http://www.fdsn.org/webservices/\">FDSN Web Services</a> spec. See below for more deetails.</div><P><BR><P>\n");
    output.append("<div id=\"tabContainer\">\n");

    output.append("<div id=\"tabs\">\n");
    output.append("<ul>\n");
    int i = 1;
    output.append("<li id=\"tabHeader_" + i++ + "\">URL Builder</li>\n");
    output.append("<li id=\"tabHeader_" + i++ + "\">Service Description</li>\n");

    output.append("</ul>\n");
    output.append("</div>\n");

    output.append("<div id=\"tabscontent\">\n");
    i = 1;

    output.append("<div class=\"tabpage\" id=\"tabpage_" + i++ + "\">");

    output.append(getFileAsString(UrlBuillderTemplate));
    output.append("</div>\n");

    output.append("<div class=\"tabpage\" id=\"tabpage_" + i++ + "\">");
    output.append(getFileAsString(InterfaceDescriptionTemplate));
    output.append("</div>\n");

    output.append("</div>\n");
    output.append("</div>\n");
    output.append("<p><br><p><b>" + WWS.getVersion() + "</b>\n");
    output.append("</div><script src=\"/tabs.js\"></script>\n");
    output.append("</body></html>\n");

    final String html = output.toString();
    final HttpResponse response = new HttpResponse("text/html; charset=utf-8");
    response.setLength(html.length());
    response.setCode("200");

    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(html, socketChannel);
  }

  protected String getFileAsString(final String file) {
    final InputStream in = this.getClass().getClassLoader().getResourceAsStream(file);
    final StringBuilder inputStringBuilder = new StringBuilder();
    BufferedReader bufferedReader;
    try {
      bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      String line = bufferedReader.readLine();
      while (line != null) {
        inputStringBuilder.append(line + "\n");
        line = bufferedReader.readLine();
      }
    } catch (final UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IOException e) {

    }

    String html = inputStringBuilder.toString();
    html = html.replace("%%HOST%%", request.getHeader("Host"));
    return html;
  }

}
