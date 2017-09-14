package gov.usgs.volcanoes.winston.legacyServer.cmd.http;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.TimeZone;

import gov.usgs.math.DownsamplingType;
import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.decorate.DefaultFrameDecorator;
import gov.usgs.plot.decorate.DefaultFrameDecorator.Location;
import gov.usgs.plot.render.MatrixRenderer;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;

/**
 * Return a rsam plot.
 *
 * @author Tom Parker
 *
 */
public final class HttpRsamCommand extends AbstractHttpCommand implements HttpBaseCommand {
  private static final int DEFAULT_H = 300;
  private static final int DEFAULT_W = 900;
  private static final boolean DEFAULT_DS = false;
  private static final int DEFAULT_DSP = 0;
  private static final int DEFAULT_RSAMP = 60;
  private static final boolean DEFAULT_DT = false;
  private static final String DEFAULT_T1 = "-12";
  private static final String DEFAULT_T2 = "now";
  private static final String DEFAULT_MAX = "auto";
  private static final String DEFAULT_MIN = "auto";
  private static final boolean DEFAULT_RM = false;
  private static final boolean DEFAULT_CSV = false;
  private static final int DEFAULT_RMP = 300;

  private TimeZone timeZone;
  private int timeZoneOffset;
  private Double endTime;
  private Double startTime;
  private int width;
  private int height;
  private boolean detrend;
  private int rsamPeriod;
  private boolean despike;
  private double despikePeriod;
  private boolean runningMedian;
  private double runningMedianPeriod;
  private double plotMax;
  private double plotMin;
  private boolean outputData;
  private String errorString = "";
  private String code;
  private RSAMData rsamData;

  public HttpRsamCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  private String parseArguments() {
    code = arguments.get("code");
    if (code == null)
      errorString += "Error: you must specify a channel (code).<br>";
    else {
      code = code.replace('_', '$');
      if (code.indexOf(";") != -1)
        errorString += "Error: illegal characters in channel (code).<br>";
    }

    timeZone = TimeZone.getTimeZone(StringUtils.stringToString(arguments.get("tz"), "UTC"));

    endTime = getEndTime(arguments.get("t2"));
    if (endTime.isNaN())
      errorString +=
          "Error: could not parse end time (t2). Should be " + INPUT_DATE_FORMAT + ".<br>";

    startTime = getStartTime(arguments.get("t1"), endTime, ONE_DAY);
    if (startTime.isNaN())
      errorString += "Error: could not parse start time (t1). Should be " + INPUT_DATE_FORMAT
          + " or -HH.M<br>";

    timeZoneOffset = timeZone.getOffset(J2kSec.asEpoch(endTime));

    width = StringUtils.stringToInt(arguments.get("w"), DEFAULT_W);
    height = StringUtils.stringToInt(arguments.get("h"), DEFAULT_H);

    if (height * width <= 0 || height * width > wws.httpMaxSize())
      errorString += "Error: product of width (w) and height (h) must be between 1 and "
          + wws.httpMaxSize() + ".<br>";

    detrend = StringUtils.stringToBoolean(arguments.get("dt"), DEFAULT_DT);

    rsamPeriod = StringUtils.stringToInt(arguments.get("rsamP"), DEFAULT_RSAMP);

    despike = StringUtils.stringToBoolean(arguments.get("ds"), DEFAULT_DS);
    despikePeriod = StringUtils.stringToDouble(arguments.get("dsp"), 0);

    runningMedian = StringUtils.stringToBoolean(arguments.get("rm"), false);
    runningMedianPeriod = StringUtils.stringToDouble(arguments.get("rmp"), 300);

    plotMax = StringUtils.stringToDouble(arguments.get("max"), Double.MAX_VALUE);
    plotMin = StringUtils.stringToDouble(arguments.get("min"), Double.MIN_VALUE);

    outputData = StringUtils.stringToBoolean(arguments.get("csv"), DEFAULT_CSV);

    return errorString;
  }

  private void getData() {
    rsamData = null;
    try {

      DownsamplingType dst;
      int dsInt;
      if (rsamPeriod < 2) {
        dst = DownsamplingType.NONE;
        dsInt = 0;
      } else {
        dst = DownsamplingType.MEAN;
        dsInt = rsamPeriod;
      }
      Scnl scnl = Scnl.parse(code);
      rsamData = data.getRSAMData(scnl, startTime, endTime, 0, dst, dsInt);
      rsamData.adjustTime(timeZoneOffset);

      if (despike)
        rsamData.despike(1, despikePeriod);

      if (detrend)
        rsamData.detrend(1);

      if (runningMedian)
        rsamData.set2median(1, runningMedianPeriod);


    } catch (final UtilException e) {
      writeSimpleHTML("Error: could not get RSAM data, check channel (code). e = " + e.toString());
    }
    if (rsamData == null || rsamData.rows() <= 0)
      writeSimpleHTML("Error: could not get RSAM data, check channel (code). Empty result.");
  }

  private void sendPlot() {
    final HttpResponse response = new HttpResponse("image/png");
    response.setVersion(request.getVersion());
    if (wws.httpRefreshInterval() > 0)
      response.setHeader("Refresh:", wws.httpRefreshInterval() + "; url=" + request.getResource());

    byte[] png;

    final Plot plot = new Plot();
    plot.setSize(width, height);
    plot.setBackgroundColor(new Color(0.97f, 0.97f, 0.97f));

    final MatrixRenderer mr = new MatrixRenderer(rsamData.getData(), false);
    final double max = Math.min(plotMax, rsamData.max(1) + rsamData.max(1) * .1);
    final double min = Math.max(plotMin, rsamData.min(1) - rsamData.max(1) * .1);

    mr.setExtents(startTime + timeZoneOffset, endTime + timeZoneOffset, min, max);
    mr.setLocation(70, 35, width - 140, height - 70);
    mr.createDefaultAxis();
    mr.setXAxisToTime(8, true, true);

    final String tzText =
        timeZone.getDisplayName(timeZone.inDaylightTime(J2kSec.asDate(endTime)), TimeZone.SHORT);
    final String bottomText =
        "(" + J2kSec.format(DISPLAY_DATE_FORMAT, startTime + timeZoneOffset) + " to "
            + J2kSec.format(DISPLAY_DATE_FORMAT, endTime + timeZoneOffset) + " " + tzText + ")";

    mr.getAxis().setBottomLabelAsText(bottomText);
    mr.getAxis().setLeftLabelAsText("RSAM");
    DefaultFrameDecorator.addLabel(mr, code.replace('$', ' '), Location.LEFT);
    mr.createDefaultLineRenderers(Color.blue);
    // mr.setExtents(startTime, endTime, gdm.min(1), gdm.max(1));
    plot.addRenderer(mr);
    try {
      png = plot.getPNGBytes();
      response.setLength(png.length);
      netTools.writeString(response.getHeaderString(), socketChannel);
      netTools.writeByteBuffer(ByteBuffer.wrap(png), socketChannel);
    } catch (final PlotException e) {
      e.printStackTrace();
      errorString = errorString + "Error: Can not create plot.";
    }

  }

  private void sendData() {
    final String html = rsamData.toCSV();
    final HttpResponse response = new HttpResponse("text/csv; charset=utf-8");
    final String fileName = code + "-RSAM.csv";
    response.setHeader("content-Disposition:", "attachment; filename='" + fileName + "'");
    response.setLength(html.length());
    response.setCode("200");
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(html, socketChannel);
  }

  @Override
  protected void sendResponse() {
    errorString = parseArguments();

    if (errorString.length() > 0) {
      writeSimpleHTML(errorString);
    } else {
      getData();
      if (outputData)
        sendData();
      else
        sendPlot();
    }
  }

  public String getUsage(final HttpRequest req) {
    final StringBuilder output = new StringBuilder();

    output.append(
        "<script>function buildRsamUrl() {" + "var urlDiv = document.getElementById(\"rsamUrl\");\n"
            + "var rsamW = document.getElementById(\"rsamW\");\n"
            + "var rsamH = document.getElementById(\"rsamH\");\n"
            + "var rsamP = document.getElementById(\"rsamP\");\n"
            + "var rsamDT = document.getElementById(\"rsamDT\");\n"
            + "var rsamDS = document.getElementById(\"rsamDS\");\n"
            + "var rsamDSP = document.getElementById(\"rsamDSP\");\n"
            + "var rsamRM = document.getElementById(\"rsamRM\");\n"
            + "var rsamRMP = document.getElementById(\"rsamRMP\");\n"
            + "var rsamMIN = document.getElementById(\"rsamMIN\");\n"
            + "var rsamMAX = document.getElementById(\"rsamMAX\");\n"
            + "var rsamTZ = document.getElementById(\"rsamTZ\");\n"
            + "var rsamT1 = document.getElementById(\"rsamT1\");\n"
            + "var rsamT2 = document.getElementById(\"rsamT2\");\n"
            + "var rsamP = document.getElementById(\"rsamP\");\n"
            + "var csv = document.getElementById(\"rsamCSV\");\n"
            + "var rsamCODE = document.getElementById(\"rsamCODE\");\n"
            + "var a = document.createElement('a');\n" + "var linkUrl = \"http://"
            + req.getHeader("Host") + "/rsam?\";\n");

    output.append("linkUrl += \"code=\" + rsamCODE.value;");
    output.append("if (rsamT1.value != \"" + DEFAULT_T1
        + "\" && rsamT1.value != \"\") { linkUrl += \"&t1=\" + rsamT1.value;}\n");
    output.append("if (rsamT2.value != \"" + DEFAULT_T2
        + "\" && rsamT2.value != \"\") { linkUrl += \"&t2=\" + rsamT2.value;}\n");
    output.append("if (rsamP.value != \"" + DEFAULT_RSAMP
        + "\" && rsamP.value != \"\") { linkUrl += \"&rsamP=\" + rsamP.value;}\n");
    output.append("if (rsamW.value != \"" + DEFAULT_W
        + "\" && rsamW.value != \"\") { linkUrl += \"&w=\" + rsamW.value;}\n");
    output.append("if (rsamH.value != \"" + DEFAULT_H
        + "\" && rsamH.value != \"\") { linkUrl += \"&h=\" + rsamH.value;}\n");
    output.append("if (rsamMIN.value != \"" + DEFAULT_MIN
        + "\" && rsamMIN.value != \"\") { linkUrl += \"&min=\" + rsamMIN.value;}\n");
    output.append("if (rsamMAX.value != \"" + DEFAULT_MAX
        + "\" && rsamMAX.value != \"\") { linkUrl += \"&max=\" + rsamMAX.value;}\n");

    if (DEFAULT_DT) {
      output.append("if (rsamDT.checked == \"\") { linkUrl += \"&dt=0\";}\n");
    } else {
      output.append("if (rsamDT.checked != \"\") { linkUrl += \"&dt=1\";}\n");
    }


    if (DEFAULT_CSV) {
      output.append("if (rsamCSV.checked == \"\") { linkUrl += \"&csv=0\";}\n");
    } else {
      output.append("if (rsamCSV.checked != \"\") { linkUrl += \"&csv=1\";}\n");
    }

    if (DEFAULT_DS) {
      output.append(
          "if (rsamDS.checked == \"\") { linkUrl += \"&ds=0\"; } else if (rsamDSP.value != \""
              + DEFAULT_DSP
              + "\" && rsamDSP.value != \"\") { linkUrl += \"&dsp=\" + rsamDSP.value;}\n");
    } else {
      output.append("if (rsamDS.checked != \"\") { linkUrl += \"&ds=1\"; if (rsamDSP.value != \""
          + DEFAULT_DSP
          + "\" && rsamDSP.value != \"\") { linkUrl += \"&dsp=\" + rsamDSP.value;}}\n");
    }

    if (DEFAULT_RM) {
      output.append(
          "if (rsamRM.checked == \"\") { linkUrl += \"&rm=0\";} else if (rsamRMP.value != \""
              + DEFAULT_RMP
              + "\" && rsamRMP.value != \"\") { linkUrl += \"&rmp=\" + rsamRMP.value;}\n");
    } else {
      output.append("if (rsamRM.checked != \"\") { linkUrl += \"&rm=1\"; if (rsamRMP.value != \""
          + DEFAULT_RMP
          + "\" && rsamRMP.value != \"\") { linkUrl += \"&rmp=\" + rsamRMP.value;}}\n");
    }

    output.append(
        "if (rsamTZ.value != \"" + DEFAULT_TZ + "\") { linkUrl += \"&tz=\" + rsamTZ.value;}\n");
    output.append("linkUrl = linkUrl.replace(\"?&\", \"?\");\n");
    output.append("linkUrl = linkUrl.replace(/\\?$/, \"\");\n");
    output.append("a.href = linkUrl;\n" + "a.text = linkUrl;\n" + "a.textContent = linkUrl; \n"
        + "while(urlDiv.hasChildNodes()) {urlDiv.removeChild(urlDiv.lastChild);}"
        + "urlDiv.appendChild(a);\n"
        + "rsamDSP.disabled = !rsamDS.checked; rsamRMP.disabled = !rsamRM.checked;"
        + "}</script>\n");

    output.append("Plots precomputed RSAM values. \n");
    output.append("<div class=\"tabContentTitle\">URL Builder</div>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append("<FORM>\n");
    output.append("<div class=\"left\">\n");
    output.append("<div class=\"left\">Channel<BR>");
    output.append(
        "<select id=\"rsamCODE\" onchange=\"buildRsamUrl()\" name=\"code\" class=\"channel\" size=8></select></div>\n");
    output.append("<div class=\"right\">Time zone<br>");
    output.append(
        "<select onchange=\"buildRsamUrl()\" class=\"timeZone\" id=\"rsamTZ\" name=\"tz\" size=8>"
            + "</select></div>\n");
    output.append("<div class=\"clear\"></div>");
    output.append(
        "<P><br><p><div class=\"left\"><div class=\"timeInput\"><label for=\"t1\">Start Time</label></div><input type=text id=\"rsamT1\" onchange=\"buildRsamUrl()\" name=\"t1\" size=10 value=\""
            + DEFAULT_T1 + "\"></div><br>");
    output.append(
        "<div class=\"left\"><div class=\"timeInput\"><label for=\"t2\">End Time</label></div><input type=text id=\"rsamT2\" onchange=\"buildRsamUrl()\" name=\"t2\" size=10 value=\""
            + DEFAULT_T2 + "\"></div><br>");
    output.append("</div>\n");
    output.append("<div class=\"right\" style=\"padding-right:15px;\">\n");
    output.append(
        "<div class=\"input\"><label for=\"rsamP\">RSAM period</label><input type=text id=\"rsamP\" onchange=\"buildRsamUrl()\" name=\"rsamP\" size=3 value=\""
            + DEFAULT_RSAMP + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"w\">Width</label><input type=text id=\"rsamW\" onchange=\"buildRsamUrl()\" name=\"w\" size=3 value=\""
            + DEFAULT_W + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"h\">Height</label><input type=text id=\"rsamH\" onchange=\"buildRsamUrl()\" name=\"h\" size=3 value=\""
            + DEFAULT_H + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"max\">Plot Max</label><input type=text id=\"rsamMAX\" onchange=\"buildRsamUrl()\" name=\"max\" size=3 value=\""
            + DEFAULT_MAX + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"min\">Plot Min</label><input type=text id=\"rsamMIN\" onchange=\"buildRsamUrl()\" name=\"min\" size=3 value=\""
            + DEFAULT_MIN + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"dt\">Detrend?</label><input class=\"checkbox\" type=checkbox id=\"rsamDT\" onchange=\"buildRsamUrl()\" name=\"dt\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"ds\">Filter Spikes?</label><input class=\"checkbox\" type=checkbox id=\"rsamDS\" onchange=\"buildRsamUrl()\" name=\"ds\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"dsp\">Period</label><input type=text id=\"rsamDSP\" onchange=\"buildRsamUrl()\" name=\"pds\" size=3 value=\""
            + DEFAULT_DSP + "\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"rm\">Remove Median?</label><input class=\"checkbox\" type=checkbox id=\"rsamRM\" onchange=\"buildRsamUrl()\" name=\"rm\" checked></div>");
    output.append(
        "<div class=\"input\"><label for=\"rmp\">Period</label><input type=text id=\"rsamRMP\" onchange=\"buildRsamUrl()\" name=\"rmp\" size=3 value=\"600\"></div>");
    output.append(
        "<div class=\"input\"><label for=\"csv\">Return CSV?</label><input class=\"checkbox\" type=checkbox id=\"rsamCSV\" onchange=\"buildRsamUrl()\" name=\"csv\"></div>");
    output.append("</div><br></FORM><div class=\"clear\"></div>\n");

    output.append("<HR class=\"urlBuilder\"><b>URL:</b><BR><div id=\"rsamUrl\"></div>");
    output.append("</div>");
    output.append("<div class=\"tabContentTitle\">Arguments</div>\n");
    output.append("<div class=\"tabContent\">\n");
    output.append(
        "The options (separated by the & character, all optional except for <code>code</code> ) are defined as follows:<br><br>"
            + "<code>w</code>: <b>Width</b> in pixels of the returned image (default = " + DEFAULT_W
            + ").<br><br>"
            + "<code>h</code>: <b>Height</b> in pixels of the returned image (default = "
            + DEFAULT_H + ").<br><br>" + ""
            + "<code>t1</code>: <b>Start Time</b> The start time (local) of the rsam plot as given by the number of days before present or a specific time in the format YYYYMMDDHHMM.  Note that, in the first case, this is a negative number (default = "
            + DEFAULT_T1 + ").<br><br>" + ""
            + "<code>t2</code>: <b>End Time</b> The end time (local) of the rsam plot as given by the format YYYYMMDDHHMM or 'now' (default = "
            + DEFAULT_T2 + ").<br><br>" + ""
            + "<code>dt</code>: <b>Detrend</b> Whether to detrend (linear) the plot, 1 is yes, 0 is no (default = "
            + DEFAULT_DT + ").<br><br>"
            + "<code>ds</code>: <b>Despike</b> Whether to despike (mean) the plot, 1 is yes, 0 is no (default = "
            + DEFAULT_DS + ").<br><br>"
            + "<code>dsp</code>: <b>Despike Period</b> Period to use for despike (default = "
            + DEFAULT_DSP + ").<br><br>"
            + "<code>max</code>: <b>Plot Max</b> Largest value to plot<BR><BR>"
            + "<code>min</code>: <b>Plot Min</b> Smallest value to plot<BR><BR>"
            + "<code>csv</code>: <b>CSV</b> Whether to return a CSV file rather than a plot, 1 is yes, 0 is no (default = "
            + DEFAULT_CSV + ").<br><br>" + ""
            + "<code>rm</code>: <b>Running Median</b>Whether to apply a running median filter<br><br>"
            + "<code>rmp</code>: <b>Running Median Period</b> Period to use for running medial, in seconds (defualt = "
            + DEFAULT_RMP + ")<br><br>" + ""
            + "<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below.<br><br>");
    output.append("</div>");
    return output.toString();
  }

  public String getAnchor() {
    return "rsam";
  }

  public String getTitle() {
    return "RSAM";
  }

  @Override
  public String getCommand() {
    return "/rsam";
  }
}
