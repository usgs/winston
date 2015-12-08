package gov.usgs.volcanoes.winston.server.cmd.http;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import gov.usgs.net.Connections;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 * Return server status. Mostly intended to give an overview of data freshness.
 *
 * @author Tom Parker
 *
 */
public final class HttpStatusCommand extends AbstractHttpCommand implements HttpBaseCommand {

  private static Connections connections = Connections.getInstance();

  public HttpStatusCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {
    final DecimalFormat formatter = new DecimalFormat("#.##");
    final double now = J2kSec.fromEpoch(System.currentTimeMillis());

    final Channels channels = new Channels(winston);
    final List<Channel> sts = channels.getChannelsByLastInsert();
    for (int i = 0; i < sts.size(); i++) {
      final Channel chan = sts.get(i);
      if (wws.getMaxDays() > 0 && chan.getMaxTime() < now - (wws.getMaxDays() * (60 * 60 * 24))) {
        sts.remove(i);
        i--;
      }
    }


    final double medianDataAge = now - sts.get(sts.size() / 2).getMaxTime();

    final ArrayList<String> oneMinChannels = new ArrayList<String>();
    final ArrayList<String> fiveMinChannels = new ArrayList<String>();
    final ArrayList<String> oneHourChannels = new ArrayList<String>();
    final ArrayList<String> oneDayChannels = new ArrayList<String>();
    final ArrayList<String> oneMonthChannels = new ArrayList<String>();
    final ArrayList<String> ancientChannels = new ArrayList<String>();

    for (final Channel chan : sts) {
      final double age = now - chan.getMaxTime();
      final String code = chan.getCode().replace('$', '_');
      if (age < 60)
        oneMinChannels.add(code);
      else if (age <= 60 * 5)
        fiveMinChannels.add(code);
      else if (age <= 60 * 60)
        oneHourChannels.add(code);
      else if (age <= 60 * 60 * 24)
        oneDayChannels.add(code);
      else if (age <= 60 * 60 * 24 * 7 * 4)
        oneMonthChannels.add(code);
      else
        ancientChannels.add(code);
    }

    final StringBuilder output = new StringBuilder();
    output.append("<HTML><HEAD><TITLE>Winston Status</TITLE>\n"
        + "<script language=\"javascript\" type=\"text/javascript\">\n"
        + "<!-- \n function popup(title, stations) { \n"
        + "newwindow=window.open('','','width=300,height=500,scrollbars=1,resizable=1');\n"
        + "var tmp = newwindow.document;\n"
        + "tmp.write('<html><head><title>title</title></head>');\n"
        + "tmp.write('<body><B>' + title + '</B><pre>');\n"
        + "for (var i=0; i <stations.length; i++)\n" + "tmp.write(stations[i] + \"\\n\");\n"
        + "tmp.write('</pre></body></html>');\n" + "tmp.close();\n }\n" + "// -->"
        + "</script></HEAD><BODY>\n");

    output.append("<SCRIPT type=\"text/javascript\">");
    output.append("var count1Title = \"&le; 1 minute old\";");
    output.append("var count1 = new Array(");
    for (final String s : oneMinChannels)
      output.append("\"" + s + "\",");
    output.append("\"\");");

    output.append("var countFreshTitle = \"&gt; 1 minute &and; &le; 5 minutes old\";");
    output.append("var countFresh = new Array(");
    for (final String s : fiveMinChannels)
      output.append("\"" + s + "\",");
    output.append("\"\");");

    output.append("var count5Title = \"&gt; 5 minutes &and; &le; 1 hour old\";");
    output.append("var count5 = new Array(");
    for (final String s : oneHourChannels)
      output.append("\"" + s + "\",");
    output.append("\"\");");

    output.append("var countHourTitle = \"&gt; 1 hour &and; &le; 1 day old\";");
    output.append("var countHour = new Array(");
    for (final String s : oneDayChannels)
      output.append("\"" + s + "\",");
    output.append("\"\");");

    output.append("var countDayTitle = \"> 1 day &and; &le; 4 weeks old\";");
    output.append("var countDay = new Array(");
    for (final String s : oneMonthChannels)
      output.append("\"" + s + "\",");
    output.append("\"\");");

    output.append("var countMonthTitle = \"> 4 weeks old\";");
    output.append("var countMonth = new Array(");
    for (final String s : ancientChannels)
      output.append("\"" + s + "\",");
    output.append("\"\");");

    output.append("</SCRIPT>");

    int count;
    output.append("<TABLE><TR><TD VALIGN=top>");
    output.append(
        "<TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\"><TR STYLE=\"background: #eeffee;\"><TD ALIGN=center COLSPAN=2><B>Winston Status</B></TD></TR>");
    output.append("<TR STYLE=\"background: #eeeeff;\"><TD>channel count</TD><TD><A HREF=\"/menu\">"
        + sts.size() + "</A></TD></TR>");
    output.append(
        "<TR><TD>connection count</TD><TD>" + connections.getNumConnections() + "</TD></TR>");
    output.append("<TR STYLE=\"background: #eeeeff;\"><TD>median data age</TD><TD>"
        + formatter.format(medianDataAge) + " seconds</TD></TR>");
    final Channel chan = sts.get(0);
    output.append("<TR><TD>most recent</TD><TD>" + chan.getCode().replace('$', '_') + " "
        + formatter.format(now - chan.getMaxTime()) + " seconds ago</TD></TR>");
    output.append(
        "<TR STYLE=\"background: #eeffee;\"><TD ALIGN=center COLSPAN=2><B>Data Freshness</B></TD></TR>");
    count = oneMinChannels.size();
    output.append(
        "<TR STYLE=\"background: #eeeeff;\"><TD>&le; 1 minute</TD><TD><A HREF=\"javascript:popup(count1Title,count1);\">"
            + count + " channels</A> (" + formatter.format((double) 100 * count / sts.size())
            + "%)</TD></TR>");
    count = fiveMinChannels.size();
    output.append(
        "<TR><TD>&le; 5 minutes</TD><TD><A HREF=\"javascript:popup(countFreshTitle,countFresh);\">"
            + count + " channels</A> (" + formatter.format((double) 100 * count / sts.size())
            + "%)</TD></TR>");
    count = oneHourChannels.size();
    output.append(
        "<TR STYLE=\"background: #eeeeff;\"><TD>&le; 1 hour</TD><TD><A HREF=\"javascript:popup(count5Title,count5);\">"
            + count + " channels</A> (" + formatter.format((double) 100 * count / sts.size())
            + "%)</TD></TR>");
    count = oneDayChannels.size();
    output.append(
        "<TR><TD>&le; 1 day</TD><TD><A HREF=\"javascript:popup(countHourTitle,countHour);\">"
            + count + " channels</A> (" + formatter.format((double) 100 * count / sts.size())
            + "%)</TD></TR>");
    count = oneMonthChannels.size();
    output.append(
        "<TR STYLE=\"background: #eeeeff;\"><TD>&le; 4 weeks</TD><TD><A HREF=\"javascript:popup(countDayTitle,countDay);\">"
            + count + " channels</A> (" + formatter.format((double) 100 * count / sts.size())
            + "%)</TD></TR>");
    count = ancientChannels.size();
    output.append(
        "<TR><TD>&gt; 4 weeks</TD><TD><A HREF=\"javascript:popup(countMonthTitle,countMonth);\">"
            + count + " channels</A> (" + formatter.format((double) 100 * count / sts.size())
            + "%)</TD></TR>");
    output.append("</TABLE></TD><TD VALIGN=top>");
    output.append(
        "<TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\"><TR STYLE=\"background: #eeffee;\"><TD ALIGN=center COLSPAN=2><B>"
            + (oneDayChannels.size() + oneHourChannels.size())
            + " channels between <BR>5 minutes and 24 hours old</B></TD></TR>");

    int i = 0;
    for (final Channel chan1 : sts) {
      final double a = now - chan1.getMaxTime();
      if (a > (5 * 60) && a < (60 * 60 * 24)) {
        final String bg = i++ % 2 == 0 ? "#eeeeff;" : "#ffffff;";
        output.append("<TR STYLE=\"background: " + bg + "\"><TD ALIGN=right>"
            + chan1.getCode().replace('$', '_') + "</TD><TD>" + ((int) a / 60)
            + " minutes</TD></TR>");
      }
    }

    output.append("</TR></TABLE></TD></TR></TABLE>");
    output.append("</BODY></HTML>");

    final String html = output.toString();
    final HttpResponse response = new HttpResponse("text/html");
    response.setLength(html.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(html, socketChannel);

  }

  public String getUsage(final HttpRequest req) {
    final String url = "http://" + req.getHeader("Host") + "/status";

    final StringBuilder output = new StringBuilder();

    output.append(
        "Returns server status. The status page details on server connections and dataflow into Winston.\n");
    output.append("<div class=\"tabContentTitle\">URL Builder</DIV>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("<HR class=\"urlBuilder\"><b>URL:</b><BR><div id=\"statusUrl\"><a href=\"" + url
        + "\">" + url + "</a></div>");
    output.append("</div>");
    output.append("<div class=\"tabContentTitle\">Arguments</DIV>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("None.\n");
    output.append("</div>");
    return output.toString();

  }

  public String getAnchor() {
    return "status";
  }

  public String getTitle() {
    return "WWS Status";
  }

  @Override
  public String getCommand() {
    return "/status";
  }
}
