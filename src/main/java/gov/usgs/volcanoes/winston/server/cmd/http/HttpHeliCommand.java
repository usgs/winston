package gov.usgs.volcanoes.winston.server.cmd.http;

import java.nio.ByteBuffer;
import java.util.TimeZone;
import java.util.logging.Level;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.plot.HelicorderSettings;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 * Return a Helicorder plot.
 *
 * @author Tom Parker
 *
 */
public final class HttpHeliCommand extends AbstractHttpCommand implements HttpBaseCommand {
  private static final int MAX_HOURS = 144;
  private static final int MIN_HOURS = 1;
  private static final int DEFAULT_H = 800;
  private static final int DEFAULT_W = 800;
  private static final int DEFAULT_TC = 30;
  private static final double MAX_TC = 21600;
  private static final boolean DEFAULT_SC = true;
  private static final boolean DEFAULT_FC = false;
  private static final boolean DEFAULT_LB = false;
  private static final String DEFAULT_BR = "auto";
  private static final String DEFAULT_CV = "auto";
  private static final String DEFAULT_T1 = "-12";
  private static final String DEFAULT_T2 = "now";

  public HttpHeliCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {

    final CodeTimer ct = new CodeTimer("HttpHeliCommand");

    String error = "";
    final HelicorderSettings settings = new HelicorderSettings();

    settings.channel = arguments.get("code");
    if (settings.channel == null)
      error = "Error: you must specify a channel (code).";
    else {
      settings.channel = settings.channel.replace('_', '$');
      if (settings.channel.indexOf(";") != -1)
        error = "Error: illegal characters in channel (code).";
    }

    final String tz = Util.stringToString(arguments.get("tz"), DEFAULT_TZ);
    settings.timeZone = TimeZone.getTimeZone(tz);

    settings.endTime = getEndTime(arguments.get("t2"));
    if (settings.endTime == Double.NaN)
      error = error + "Error: could not parse end time (t2). Should be " + INPUT_DATE_FORMAT + ".";

    settings.startTime = getStartTime(arguments.get("t1"), settings.endTime, ONE_HOUR);
    if (settings.startTime == Double.NaN)
      error += "Error: cannot parse start time. Should be " + INPUT_DATE_FORMAT
          + " or -HH. I received " + arguments.get("t1");

    if (settings.endTime - settings.startTime > MAX_HOURS * ONE_HOUR)
      error += "Error: Plot must not be more that " + MAX_HOURS + " hours long";
    else if (settings.endTime - settings.startTime < MIN_HOURS * ONE_HOUR)
      error += "Error: Plot cannot be less than " + MIN_HOURS + " hour long";

    settings.timeChunk = Util.stringToDouble(arguments.get("tc"), DEFAULT_TC) * ONE_MINUTE;
    if (settings.timeChunk <= 0 || settings.timeChunk > MAX_TC)
      error = error + "Error: time chunk (tc) must be greater than 0 and less than " + MAX_TC + ".";

    final int width = Util.stringToInt(arguments.get("w"), DEFAULT_W);
    final int height = Util.stringToInt(arguments.get("h"), DEFAULT_H);
    settings.setSizeFromPlotSize(width, height);

    if (settings.height * settings.width <= 0
        || settings.height * settings.width > wws.httpMaxSize())
      error = error + "Error: product of width (w) and height (h) must be between 1 and "
          + wws.httpMaxSize() + ".";

    settings.showClip = Util.stringToBoolean(arguments.get("sc"), DEFAULT_SC);
    settings.forceCenter = Util.stringToBoolean(arguments.get("fc"), DEFAULT_FC);
    settings.barRange = Util.stringToInt(arguments.get("br"), -1);
    settings.clipValue = Util.stringToInt(arguments.get("cv"), -1);

    settings.largeChannelDisplay = Util.stringToBoolean(arguments.get("lb"));

    settings.minimumAxis = Util.stringToBoolean(arguments.get("min"));
    if (settings.minimumAxis)
      settings.setMinimumSizes();

    if (error.length() > 0) {
      ct.stop();
      writeSimpleHTML(error);
    } else {
      HelicorderData heliData = null;
      try {
        heliData =
            data.getHelicorderData(settings.channel, settings.startTime, settings.endTime, 0);
      } catch (final UtilException e) {
      }
      ct.stop();

      // Did it take too long to gather the data?
      if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
        wws.log(Level.INFO,
            String.format(
                "slow db query (%1.2f ms) http/heli " + settings.channel + " " + settings.startTime
                    + " -> " + settings.endTime + " ("
                    + decimalFormat.format(settings.endTime - settings.startTime) + ") ",
                ct.getRunTimeMillis()),
            socketChannel);

      if (heliData == null || heliData.rows() <= 0)
        writeSimpleHTML("Error: could not get helicorder data, check channel (code).");
      else {
        ct.start();
        final HttpResponse response = new HttpResponse("image/png");
        response.setVersion(request.getVersion());
        if (wws.httpRefreshInterval() > 0)
          response.setHeader("Refresh:",
              wws.httpRefreshInterval() + "; url=" + request.getResource());

        byte[] png;
        try {
          png = settings.createPlot(heliData).getPNGBytes();
          response.setLength(png.length);
          netTools.writeString(response.getHeaderString(), socketChannel);
          netTools.writeByteBuffer(ByteBuffer.wrap(png), socketChannel);
        } catch (final PlotException e) {
          e.printStackTrace();
          error = error + "Error: Can not create plot.";
        }

        ct.stop();
        // Did it take too long to deliver the data?
        if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
          wws.log(Level.INFO,
              String.format(
                  "slow network (%1.2f ms) http/heli? " + settings.channel + " "
                      + settings.startTime + " -> " + settings.endTime + " ("
                      + decimalFormat.format(settings.endTime - settings.startTime) + ") ",
                  ct.getRunTimeMillis()),
              socketChannel);

      }
    }
  }

  public String getUsage(final HttpRequest req) {
    final StringBuilder output = new StringBuilder();

    output.append(
        "<script>function buildHeliUrl() {" + "var urlDiv = document.getElementById(\"heliUrl\");\n"
            + "var heliW = document.getElementById(\"heliW\");\n"
            + "var heliH = document.getElementById(\"heliH\");\n"
            + "var heliTC = document.getElementById(\"heliTC\");\n"
            + "var heliBR = document.getElementById(\"heliBR\");\n"
            + "var heliCV = document.getElementById(\"heliCV\");\n"
            + "var heliSC = document.getElementById(\"heliSC\");\n"
            + "var heliFC = document.getElementById(\"heliFC\");\n"
            + "var heliLB = document.getElementById(\"heliLB\");\n"
            + "var heliTZ = document.getElementById(\"heliTZ\");\n"
            + "var heliT1 = document.getElementById(\"heliT1\");\n"
            + "var heliT2 = document.getElementById(\"heliT2\");\n"
            + "var heliCODE = document.getElementById(\"heliCODE\");\n"
            + "var a = document.createElement('a');\n" + "var linkUrl = \"http://"
            + req.getHeader("Host") + "/heli?\";\n");

    output.append("linkUrl += \"code=\" + heliCODE.value;");
    output.append("if (heliT1.value != \"" + DEFAULT_T1
        + "\" && heliT1.value != \"\") { linkUrl += \"&t1=\" + heliT1.value;}\n");
    output.append("if (heliT2.value != \"" + DEFAULT_T2
        + "\" && heliT2.value != \"\") { linkUrl += \"&t2=\" + heliT2.value;}\n");
    output.append("if (heliW.value != \"" + DEFAULT_W
        + "\" && heliW.value != \"\") { linkUrl += \"&w=\" + heliW.value;}\n");
    output.append("if (heliH.value != \"" + DEFAULT_H
        + "\" && heliH.value != \"\") { linkUrl += \"&h=\" + heliH.value;}\n");
    output.append("if (heliTC.value != \"" + DEFAULT_TC
        + "\" && heliTC.value != \"\") { linkUrl += \"&tc=\" + heliTC.value;}\n");
    output.append("if (heliBR.value != \"" + DEFAULT_BR
        + "\" && heliBR.value != \"\") { linkUrl += \"&br=\" + heliBR.value;}\n");
    output.append("if (heliCV.value != \"" + DEFAULT_CV
        + "\" && heliCV.value != \"\") { linkUrl += \"&cv=\" + heliCV.value;}\n");

    if (DEFAULT_SC) {
      output.append("if (heliSC.checked == \"\") { linkUrl += \"&sc=0\";}\n");
    } else {
      output.append("if (heliSC.checked != \"\") { linkUrl += \"&sc=1\";}\n");
    }

    if (DEFAULT_FC) {
      output.append("if (heliFC.checked == \"\") { linkUrl += \"&fc=0\";}\n");
    } else {
      output.append("if (heliFC.checked != \"\") { linkUrl += \"&fc=1\";}\n");
    }

    if (DEFAULT_LB) {
      output.append("if (heliLB.checked == \"\") { linkUrl += \"&lb=0\";}\n");
    } else {
      output.append("if (heliLB.checked != \"\") { linkUrl += \"&lb=1\";}\n");
    }

    output.append(
        "if (heliTZ.value != \"" + DEFAULT_TZ + "\") { linkUrl += \"&tz=\" + heliTZ.value;}\n");
    output.append("linkUrl = linkUrl.replace(\"?&\", \"?\");\n");
    output.append("linkUrl = linkUrl.replace(/\\?$/, \"\");\n");
    output.append("a.href = linkUrl;\n" + "a.text = linkUrl;\n" + "a.textContent = linkUrl; \n"
        + "while(urlDiv.hasChildNodes()) {urlDiv.removeChild(urlDiv.lastChild);}"
        + "urlDiv.appendChild(a);\n" + "heliCV.disabled = !heliSC.checked;" + "}</script>\n");

    output.append("Returns a helicorder plot. \n");
    output.append("<div class=\"tabContentTitle\">URL Builder</div>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("<FORM>\n");
    output.append("<div class=\"left\">\n");
    output.append("<div class=\"left\">Channel<BR>");
    output.append(
        "<select id=\"heliCODE\" onchange=\"buildHeliUrl()\" name=\"code\" class=\"channel\" size=8></select></div>\n");
    output.append("<div class=\"right\">Time zone<br>");
    output.append(
        "<select onchange=\"buildHeliUrl()\" class=\"timeZone\" id=\"heliTZ\" name=\"tz\" size=8>"
            + "</select></div>\n");
    output.append("<div class=\"clear\"></div>");
    output.append(
        "<P><br><p><div class=\"left\"><div class=\"timeInput\"><label for=\"t1\">Start Time</label></div><input type=text id=\"heliT1\" onchange=\"buildHeliUrl()\" name=\"t1\" size=10 value=\""
            + DEFAULT_T1 + "\"></div><br>");
    output.append("<div class=\"clear\"></div>\n");
    output.append(
        "<div class=\"left\"><div class=\"timeInput\"><label for=\"t2\">End Time</label></div><input type=text id=\"heliT2\" onchange=\"buildHeliUrl()\" name=\"t2\" size=10 value=\""
            + DEFAULT_T2 + "\"></div><br>");
    output.append("</div>\n");
    output.append("<div class=\"right\" style=\"padding-right:15px;\">\n");
    output.append(
        "<div class=\"input\"><label for=\"w\">Width</label><input type=text id=\"heliW\" onchange=\"buildHeliUrl()\" name=\"w\" size=3 value=\""
            + DEFAULT_W + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"h\">Height</label><input type=text id=\"heliH\" onchange=\"buildHeliUrl()\" name=\"h\" size=3 value=\""
            + DEFAULT_H + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"tc\">Time Chunk</label><input type=text id=\"heliTC\" onchange=\"buildHeliUrl()\" name=\"tc\" size=3 value=\""
            + DEFAULT_TC + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"br\">Bar Range</label><input type=text id=\"heliBR\" onchange=\"buildHeliUrl()\" name=\"br\" size=3 value=\""
            + DEFAULT_BR + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"sc\">Show clip?</label><input class=\"checkbox\" type=checkbox id=\"heliSC\" onchange=\"buildHeliUrl()\" name=\"sc\" checked></div>");
    output.append(
        "<div class=\"input\"><label for=\"cv\">Clip Value</label><input type=text id=\"heliCV\" onchange=\"buildHeliUrl()\" name=\"cv\" size=3 value=\""
            + DEFAULT_CV + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"fc\">Force center?</label><input class=\"checkbox\" type=checkbox id=\"heliFC\" onchange=\"buildHeliUrl()\" name=\"fc\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"fc\">Display Label?</label><input class=\"checkbox\" type=checkbox id=\"heliLB\" onchange=\"buildHeliUrl()\" name=\"lb\" checked></div>");
    output.append("</div></FORM><div class=\"clear\"></div>\n");
    output.append("<HR class=\"urlBuilder\"><b>URL:</b><BR><div id=\"heliUrl\"></div>");
    output.append("</div>");
    output.append("<div class=\"tabContentTitle\">Arguments</div>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("<code>w</code>: <b>Width</b> in pixels of the returned image (default = "
        + DEFAULT_W + ").<br><br>"
        + "<code>h</code>: <b>Height</b> in pixels of the returned image (default = " + DEFAULT_H
        + ").<br><br>"
        + "<code>tc</code>: <b>Time Chunk</b> length of x axis in minutes (default = " + DEFAULT_TC
        + ").<br><br>" + ""
        + "<code>t1</code>: <b>Start Time</b> The start time (local) of the helicorder as given by the number of hours before present or a specific time in the format YYYYMMDDHHMM.  Note that, in the first case, this is a negative number (default = -12).<br><br>"
        + ""
        + "<code>t2</code>: <b>End Time</b> The end time (local) of the helicorder as given by the format YYYYMMDDHHMM or 'now' (default = 'now').<br><br>"
        + ""
        + "<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below.<br><br>"
        + "<code>sc</code>: <b>Show Clip</b> Whether to show a clipped value as red, 1 is yes, 0 is no (default = "
        + boolToInt(DEFAULT_SC) + ").<br><br>"
        + "<code>fc</code>: <b>Force Center</b> Whether to center traces, 1 is yes, 0 is no (default = "
        + boolToInt(DEFAULT_FC) + ").<br><br>"
        + "<code>br</code>: <b>Bar Range</b> Controls the size of helicorder lines (default = auto).<br><br>"
        + "<code>cv</code>: <b>Clip Value</b> Sets the number of counts above which to clip (default = auto).<br><br>"
        + "<code>lb</code>: <b>Label</b> Whether to display a large label, 1 is yes, 0 is no (default = 0).<br><br>"
        + "The WWS does basic argument checking to prevent malignant attacks (like SQL injection) or just silly requests (like a 10000 x 10000 pixel graph).");
    output.append("</div>");
    return output.toString();
  }

  public String getAnchor() {
    return "heli";
  }

  public String getTitle() {
    return "Helicorder";
  }

  @Override
  public String getCommand() {
    return "/heli";
  }
}
