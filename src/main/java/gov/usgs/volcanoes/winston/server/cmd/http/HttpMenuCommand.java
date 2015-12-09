package gov.usgs.volcanoes.winston.server.cmd.http;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 * Return the wave server menu. Similar to earthworm getmenu command.
 *
 * @author Tom Parker
 *
 */
public final class HttpMenuCommand extends AbstractHttpCommand implements HttpBaseCommand {

  private static int DEFAULT_OB = 2;
  private static String DEFAULT_SO = "a";

  public HttpMenuCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {
    // validate input. Write error and return if bad.
    final int sortCol = StringUtils.stringToInt(arguments.get("ob"), DEFAULT_OB);
    if (sortCol < 1 || sortCol > 8) {
      writeSimpleHTML("Error: could not parse ob = " + arguments.get("ob"));
      return;
    }

    final String o = StringUtils.stringToString(arguments.get("so"), DEFAULT_SO);
    final char order = o.charAt(0);
    if (order != 'a' && order != 'd') {
      writeSimpleHTML("Error: could not parse so = " + arguments.get("so"));
      return;
    }

    final String tz = StringUtils.stringToString(arguments.get("tz"), DEFAULT_TZ);
    final TimeZone timeZone = TimeZone.getTimeZone(tz);

    // write header
    final String[] colTitle = {null, "Pin", "S", "C", "N", "L", "Earliest", "Most Recent", "Type"};
    colTitle[sortCol] += order == 'a' ? " &#9652;" : " &#9662;";

    final char[] colOrd = new char[colTitle.length];
    Arrays.fill(colOrd, 'a');
    if (order == 'a')
      colOrd[sortCol] = 'd';

    final StringBuilder output = new StringBuilder();

    output.append("<table CELLPADDING=\"5\"><tr>");
    for (int i = 1; i < colTitle.length; i++)
      output.append(
          "<th><a href=\"?ob=" + i + "&so=" + colOrd[i] + "\">" + colTitle[i] + "</a></th>");

    output.append("</tr>");

    // get and sort menu
    final List<String> list = emulator.getWaveServerMenu(true, 0, 0, maxDays);
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
    writeSimpleHTML(output.toString());
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

  public String getUsage(final HttpRequest req) {
    final String Url = "http://" + req.getHeader("Host") + "/menu";

    final StringBuilder output = new StringBuilder();

    output.append(
        "<script>function buildMenuUrl() {" + "var urlDiv = document.getElementById(\"menuUrl\");\n"
            + "var menuOB = document.getElementById(\"menuOB\");\n"
            + "var menuSO = document.getElementById(\"menuSO\");\n"
            + "var menuTZ = document.getElementById(\"menuTZ\");\n"
            + "var a = document.createElement('a');\n" + "var linkUrl = \"http://"
            + req.getHeader("Host") + "/menu?\";\n");

    output.append(
        "if (menuOB.value != \"" + DEFAULT_OB + "\") { linkUrl += \"&ob=\" + menuOB.value;}\n");
    output.append(
        "if (menuSO.value != \"" + DEFAULT_SO + "\") { linkUrl += \"&so=\" + menuSO.value;}\n");
    output.append(
        "if (menuTZ.value != \"" + DEFAULT_TZ + "\") { linkUrl += \"&tz=\" + menuTZ.value;}\n");
    output.append("linkUrl = linkUrl.replace(\"?&\", \"?\");\n");
    output.append("linkUrl = linkUrl.replace(/\\?$/, \"\");\n");
    output.append("a.href = linkUrl;\n" + "a.text = linkUrl;\n" + "a.textContent = linkUrl; \n"
        + "a.textContent = linkUrl; \n"
        + "while(urlDiv.hasChildNodes()) {urlDiv.removeChild(urlDiv.lastChild);}"
        + "urlDiv.appendChild(a);\n" + "}</script>\n");

    output.append(
        "Returns the server menu. The menu contains the list of stations in this winston along with the earliest and most recent data point for each station.\n");
    output.append("<div class=\"tabContentTitle\">URL Builder</DIV>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("<FORM>\n");
    output.append("<div class=\"left\"><div class=\"left\">\n");
    output.append("<label for=\"tz\">Time Zone</label><br>\n");
    output.append(
        "<select onchange=\"buildMenuUrl()\" class=\"timeZone\" id=\"menuTZ\" name=\"tz\" size=8>"
            + "</select>");
    output.append("</div><div class=\"right\">\n");
    output.append(
        "<br><div class=\"input\" style=\"width: 20em\"><label for=\"ob\">Order By</label>");
    output.append("<select id=\"menuOB\" onchange=\"buildMenuUrl()\" name=\"ob\">\n"
        + "<option value=1>Pin</option>" + "<option value=2 selected>Station</option>"
        + "<option value=3>Component</option>" + "<option value=4>Network</option>"
        + "<option value=5>Location</option>" + "<option value=6>Earliest</option>"
        + "<option value=7>Most Recent</option>" + "<option value=8>Type</Option>"
        + "</select></div><br>\n");
    output
        .append("<div class=\"input\" style=\"width: 20em\"><label for=\"so\">Sort Order</label>");
    output.append("<select id=\"menuSO\" onchange=\"buildMenuUrl()\" name=\"so\">"
        + "<option value=\"a\" selected>Ascending</option>"
        + "<option value=\"d\">Descending</option>" + "</select></div><br>");

    output.append("</div></div></FORM><div class=\"clear\"></div>\n");
    output.append("<HR class=\"urlBuilder\"><b>URL:</b><BR><div id=\"menuUrl\"></div>");
    output.append("</div>");
    output.append("<div class=\"tabContentTitle\">Arguments</DIV>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append(
        "<code>ob</code>: <b>Order By</b> The column number used to order the menu (default = "
            + DEFAULT_OB + ").<br><br>\n"
            + "<code>so</code>: <b>Sort Order</b> How to order the menu, a is ascending, d is decending (default = "
            + DEFAULT_SO + ").<br><br>\n" + "<code>tz</code>: <b>Time Zone</b> (deafult = "
            + DEFAULT_TZ + ")<br><br>\n");
    output.append("</div>");
    output.append("<div class=\"tabContentTitle\">Examples</DIV>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("Most recently added channels:<br><a href=\"http://" + req.getHeader("Host")
        + "/menu?ob=6&so=d\">" + "http://" + req.getHeader("Host")
        + "/menu?ob=6&so=d</a><br><p><br>");
    output.append("Channels with least recent data:<br><a href=\"http://" + req.getHeader("Host")
        + "/menu?ob=7&so=a\">" + "http://" + req.getHeader("Host") + "/menu?ob=7&so=a</a><p>");
    output.append("</div>");
    return output.toString();
  }

  public String getAnchor() {
    return "menu";
  }

  public String getTitle() {
    return "Server Menu";
  }

  @Override
  public String getCommand() {
    return "/menu";
  }
}
