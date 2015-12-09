package gov.usgs.volcanoes.winston.server.cmd.http;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 * Returns data gaps, gap duration, and gap analysis.
 *
 * @author Austin Sedita
 *
 */

public final class HttpGapsCommand extends AbstractHttpCommand implements HttpBaseCommand {

  private static final String DEFAULT_T1 = "-12";
  private static final String DEFAULT_T2 = "now";
  private static final boolean DEFAULT_WC = false;
  private static final int DEFAULT_MGD = 5;

  private static final double EPSILON = 1e-6
      ;
  DecimalFormat formatter = new DecimalFormat("#.###");
  double now = J2kSec.now();

  String error = "";
  String code;
  Double endTime;
  Double startTime;
  String timeZone;
  TimeZone zone;
  Double minGapDuration;
  int writeComputer;

  String startTimeS;
  String endTimeE;
  double totalTime;

  List<double[]> gaps;
  double[] gap;
  int gapCount;

  // double dataStart;
  // double dataEnd;
  // double dataLength;

  String[] color = {"#ffeeee;", "#eeffee;", "#eeeeff;"};
  int colorCount;

  double gapLength;
  double totalGapLength;
  double averageGapLength;
  double dataPercentage;

  public HttpGapsCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {

    // Station
    code = arguments.get("code");
    if (code == null) {
      error = "Error: you must specify a channel (code).";
    } else {
      code = code.replace('_', '$');
      if (code.indexOf(";") != -1) {
        error = "Error: illegal characters in channel (code).";
      }
    }

    // End Time
    endTime = getEndTime(arguments.get("t2"));
    if (endTime.isNaN()) {
      error = error + "Error: could not parse end time (t2). Should be " + INPUT_DATE_FORMAT + ".";
    }

    // Start Time
    startTime = getStartTime(arguments.get("t1"), endTime, ONE_HOUR);
    if (startTime.isNaN()) {
      error = error + "Error: could not parse start time (t1). Should be " + INPUT_DATE_FORMAT
          + " or -HH.";
    }

    // TimeZone
    timeZone = StringUtils.stringToString(arguments.get("tz"), "UTC");
    zone = TimeZone.getTimeZone(timeZone);

    // Minimum Gap Duration
    minGapDuration = StringUtils.stringToDouble(arguments.get("mgd"), 5);

    // Write Computer
    writeComputer = StringUtils.stringToInt(arguments.get("wc"), 0);

    // If Error
    if (error.length() > 0) {
      writeSimpleHTML(error);

    } else {

      SimpleDateFormat dateF = new SimpleDateFormat(Time.STANDARD_TIME_FORMAT);
      dateF.setTimeZone(zone);
      // Finds Gaps
      gaps = data.findGaps(code, startTime, endTime);
      code = code.replace('$', '_');
      startTimeS = dateF.format(J2kSec.asEpoch(startTime));
      endTimeE = dateF.format(J2kSec.asEpoch(startTime));
      totalTime = endTime - startTime;

      if (writeComputer == 1) {
        writeComputer();
      } else {
        writeHuman();
      }
    }
  }

  // Write for Humans
  void writeHuman() {

    final StringBuilder header = new StringBuilder();
    final StringBuilder outputSettings_Analysis = new StringBuilder();
    // StringBuilder outputData = new StringBuilder();
    final StringBuilder outputDataGaps = new StringBuilder();
    final StringBuilder footer = new StringBuilder();

    // Write Human Header
    header.append("<HTML><TITLE>Winston Gaps</TITLE>");
    header.append(
        "<BODY><TABLE CELLSPACING=5 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\">"
            + "<THEAD><TD ALIGN=center COLSPAN=3><BIG><B><U>Winston Gaps</U></B></BIG></TD></THEAD><TBODY><TR>");

    // Write Human Settings
    outputSettings_Analysis.append(
        "<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=400>"
            + "<THEAD><TR><TH ALIGN=center COLSPAN=2><B>Settings</B></TH></TR></THEAD>");
    outputSettings_Analysis
        .append("<TBODY><TR><TD>Start Time:</TD><TD>" + startTimeS + "</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>End Time:</TD><TD>" + endTimeE + "</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Duration:</TD><TD>" + totalTime + " seconds</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Time Zone:</TD><TD>" + timeZone + "</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Station Name:</TD><TD>" + code + "</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Minimum Gap Duration:</TD><TD>" + minGapDuration
        + " seconds</TD></TR></TBODY></TABLE>");

    // // Write Human Data
    // outputData
    // .append("<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2;
    // border-style: solid;\" WIDTH=500>"
    // +
    // "<THEAD><TR><TH ALIGN=center COLSPAN=3><B>Data</B></TH></TR></THEAD>");
    // outputData
    // .append("<TR><TH>Data Start Time:</TH><TH>Data End Time:</TD><TH>Data Duration:</TH></TR>");
    // outputData.append("<TR STYLE=\"background: #ffeeee;\"><TD>"
    // + startTimeS + "</TD>");
    // colorCount = 1;
    // dataStart = startTime;
    //
    // // Listing of Data
    // for (double[] gap : gaps) {
    // gapLength = gap[1] - gap[0];
    // dataEnd = gap[0];
    //
    // dataLength = dataEnd - dataStart;
    // if (gapLength < minGapDuration) {
    // continue;
    // } else {
    // outputData.append("<TD>" + Util.j2KToDateString(gap[0], zone)
    // + "</TD>");
    // outputData.append("</TD><TD ALIGN=right>"
    // + formatter.format(dataLength) + " seconds</TD>");
    // outputData.append("<TR STYLE=\"background: "
    // + color[colorCount++ % 3] + "\"><TD>"
    // + Util.j2KToDateString(gap[1], zone));
    // }
    // dataStart = gap[1];
    // }
    // dataLength = endTime - dataStart;
    // outputData.append("</TD><TD>" + endTimeE + "</TD>"
    // + "</TD><TD ALIGN=right>" + formatter.format(dataLength)
    // + " seconds</TD>" + "</TR></TABLE></TD>");

    // Write Human Data Gaps
    
    SimpleDateFormat dateF = new SimpleDateFormat(Time.STANDARD_TIME_FORMAT);
    outputDataGaps.append(
        "<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=500>"
            + "<THEAD><TR><TH ALIGN=center COLSPAN=3><B>Data Gaps</B></TH></TR></THEAD>");
    outputDataGaps
        .append("<TR><TH>Gap Start Time</TH><TH>Gap End Time</TH><TH>Gap Duration</TH></TR>");
    gapCount = 0;
    colorCount = 0;
    totalGapLength = 0;

    // Listing of Gaps
    for (final double[] gap : gaps) {
      gapLength = gap[1] - gap[0];
      if (gapLength < minGapDuration) {
        continue;
      } else {
        outputDataGaps.append("<TR STYLE=\"background: " + color[colorCount++ % 3] + "\"><TD>"
            + dateF.format(J2kSec.asEpoch(gap[0])) + "</TD><TD>" + dateF.format(J2kSec.asEpoch(gap[1]))
            + "</TD><TD ALIGN=right>" + formatter.format(gapLength) + " seconds</TD></TR>");
        gapCount++;
        totalGapLength = totalGapLength + gapLength;
      }
    }

    if (gapCount > 0)
      averageGapLength = totalGapLength / gapCount;
    else
      averageGapLength = 0;

    dataPercentage = (totalTime - totalGapLength) / totalTime * 100;
    outputDataGaps.append("</TABLE>");

    // Write Human Analysis
    outputSettings_Analysis.append(
        "<TABLE CELLSPACING=0 CELLPADDING=5 STYLE=\"border-width: 2; border-style: solid;\" WIDTH=400>"
            + "<THEAD><TR><TH ALIGN=center COLSPAN=2 ><B>Analysis</B></TH></TR></THEAD>");
    outputSettings_Analysis.append("<TR><TD>Number of Gaps:</TD><TD>" + gapCount + "</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Total Gap Length:</TD><TD>"
        + formatter.format(totalGapLength) + "seconds</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Average Gap Length:</TD><TD>"
        + formatter.format(averageGapLength) + "seconds</TD></TR>");
    outputSettings_Analysis.append("<TR><TD>Data percentage:</TD><TD>"
        + formatter.format(dataPercentage) + "%</TD></TR></TABLE></TD>");

    // Write Human Footer
    footer.append("</TD></TR></TBODY><TFOOT></TFOOT</TABLE></BODY></HTML>");

    // All or No Data
    if (gapCount == 0) {
      writeSimpleHTML("No Gaps for selected time frame.");
    } else if (Math.abs(totalGapLength - totalTime) < EPSILON) {
      writeSimpleHTML("No Data for selected time frame.");
    }

    // Website Creation
    final String html = header.toString() + outputSettings_Analysis.toString()
        + outputDataGaps.toString() + footer.toString();

    final HttpResponse response = new HttpResponse("text/html");
    response.setLength(html.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(html, socketChannel);
  }

  // Write for Computers
  void writeComputer() {

    final StringBuilder header = new StringBuilder();
    final StringBuilder output = new StringBuilder();
    output.append("# Gap Start\t\tGap End\t\t\tDuration\n");

    // Write Computer Data Gaps
    gapCount = 0;
    totalGapLength = 0;

    for (final double[] gap : gaps) {
      gapLength = gap[1] - gap[0];
      if (gapLength < minGapDuration) {
        continue;
      } else {
        output.append(gap[0] + "\t" + gap[1] + "\t" + gapLength + "\n");
        gapCount++;
        totalGapLength = totalGapLength + gapLength;
      }
    }

    // Write Computer Winston Gaps
    header.append("# Start Time: " + startTimeS + " (" + startTime + ")\n# End Time: " + endTimeE
        + " (" + endTime + ")\n# Total Time: " + totalTime + "\n# Time Zone: " + timeZone
        + "\n# Station: " + code + "\n# Minumum Gap Duration: " + minGapDuration + "\n# GapCount: "
        + gapCount + "\n");

    writeSimpleText(header.toString() + output.toString());
  }

  // Winston Wave Server Interface
  public String getUsage(final HttpRequest req) {
    final StringBuilder output = new StringBuilder();

    output.append(
        "<script>function buildGapsUrl() {" + "var urlDiv = document.getElementById(\"gapsUrl\");\n"
            + "var gapsWC = document.getElementById(\"gapsWC\");\n"
            + "var gapsMGD = document.getElementById(\"gapsMGD\");\n"
            + "var gapsTZ = document.getElementById(\"gapsTZ\");\n"
            + "var gapsT1 = document.getElementById(\"gapsT1\");\n"
            + "var gapsT2 = document.getElementById(\"gapsT2\");\n"
            + "var gapsCODE = document.getElementById(\"gapsCODE\");\n"
            + "var a = document.createElement('a');\n" + "var linkUrl = \"http://"
            + req.getHeader("Host") + "/gaps?\";\n");

    output.append("linkUrl += \"code=\" + gapsCODE.value;");
    output.append("if (gapsT1.value != \"" + DEFAULT_T1
        + "\" && gapsT1.value != \"\") { linkUrl += \"&t1=\" + gapsT1.value;}\n");
    output.append("if (gapsT2.value != \"" + DEFAULT_T2
        + "\" && gapsT2.value != \"\") { linkUrl += \"&t2=\" + gapsT2.value;}\n");
    output.append("if (gapsMGD.value != \"" + DEFAULT_MGD
        + "\" && gapsMGD.value != \"\") { linkUrl += \"&mgd=\" + gapsMGD.value;}\n");

    if (DEFAULT_WC) {
      output.append("if (gapsWC.checked != \"\") { linkUrl += \"&wc=0\";}\n");
    } else {
      output.append("if (gapsWC.checked == \"\") { linkUrl += \"&wc=1\";}\n");
    }

    output.append(
        "if (gapsTZ.value != \"" + DEFAULT_TZ + "\") { linkUrl += \"&tz=\" + gapsTZ.value;}\n");
    output.append("linkUrl = linkUrl.replace(\"?&\", \"?\");\n");
    output.append("linkUrl = linkUrl.replace(/\\?$/, \"\");\n");
    output.append("a.href = linkUrl;\n" + "a.text = linkUrl;\n" + "a.textContent = linkUrl; \n"
        + "while(urlDiv.hasChildNodes()) {urlDiv.removeChild(urlDiv.lastChild);}"
        + "urlDiv.appendChild(a);\n" + "}</script>\n");

    output.append("Locates gaps in continous data. \n");
    output.append("<div class=\"tabContentTitle\">URL Builder</div>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("<FORM>\n");
    output.append("<div class=\"left\">\n");
    output.append("<div class=\"left\">Channel<BR>");
    output.append(
        "<select id=\"gapsCODE\" onchange=\"buildGapsUrl()\" name=\"code\" class=\"channel\" size=8></select></div>\n");
    output.append("<div class=\"right\">Time zone<br>");
    output.append(
        "<select onchange=\"buildGapsUrl()\" class=\"timeZone\" id=\"gapsTZ\" name=\"tz\" size=8>"
            + "</select></div>\n");
    output.append("<div class=\"clear\"></div>");
    output.append(
        "<P><br><p><div class=\"label\"><label for=\"t1\">Start Time</label></div><input type=text id=\"gapsT1\" onchange=\"buildGapsUrl()\" name=\"t1\" size=10 value=\""
            + DEFAULT_T1 + "\"><br>");
    output.append(
        "<div class=\"label\"><label for=\"t2\">End Time</label></div><input type=text id=\"gapsT2\" onchange=\"buildGapsUrl()\" name=\"t2\" size=10 value=\""
            + DEFAULT_T2 + "\"><br>");
    output.append("</div>\n");
    output.append("<div class=\"right\" style=\"padding-right:15px;\">\n");
    output.append(
        "<div class=\"input\"><label for=\"mgd\">Min Gap</label><input type=text id=\"gapsMGD\" onchange=\"buildGapsUrl()\" name=\"mgd\" size=3 value=\""
            + DEFAULT_MGD + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"dt\">Human Readable?</label><input type=checkbox id=\"gapsWC\" onchange=\"buildGapsUrl()\" name=\"wc\" checked></div>");
    output.append("</div></FORM><div class=\"clear\"></div>\n");
    output.append("<HR class=\"urlBuilder\"><b>URL:</b><BR><div id=\"gapsUrl\"></div>");
    output.append("</div><div class=\"tabContentTitle\">Arguments</div>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append(
        "The options (separated by the & character, all optional except for <code>code</code> ) are defined as follows:<br><br>"
            + "This url will return data, data gaps, and analysis for the channel PS4A EHZ AV, for the last 24 hours, in Alaskan time, with a minimum gap duration of 30 seconds, written for humans.<br>"
            + "The options (separated by the & character, all optional except for code ) are defined as follows<br><br>"
            + "code: <b>Station Name</b> The name of the Station desired<br><br>"
            + "t1: <b>Start Time</b> The start time (local) of the gap analysis as given by the number of hours before present or a specific time in the format YYYYMMDDHHMM. "
            + "Note that, in the first case, this is a negative number (default = -12).<br><br>"
            + "t2: <b>End Time</b> The end time (local) of the gap analysis as given by the format YYYYMMDDHHMM or 'now' (default = 'now').<br><br>"
            + "tz: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below (default = UTC).<br><br>"
            + "mgd: <b>Minimumm Gap Duration</b> The minimum gap duration desired in seconds (default = 5)<br><br>"
            + "wc: <b>Write Computer</b> Whether to show data gaps as the computer sees, 1 is yes, 0 is no (default = 0)");
    output.append("</div>");
    return output.toString();
  }

  public String getAnchor() {
    return "gaps";
  }

  public String getTitle() {
    return "WWS Gaps";
  }

  @Override
  public String getCommand() {
    return "/gaps";
  }
}
