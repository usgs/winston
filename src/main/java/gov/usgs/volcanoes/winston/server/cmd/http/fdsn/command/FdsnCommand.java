package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command;

import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.HttpStatusCode;
import gov.usgs.net.NetTools;
import gov.usgs.util.Time;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.cmd.http.AbstractHttpCommand;

/**
 *
 * @author Tom Parker
 *
 *         TODO: fix POST argument handling
 *
 */
abstract public class FdsnCommand extends AbstractHttpCommand {

  protected String version;

  protected static SimpleDateFormat dateFormat = new SimpleDateFormat(Time.FDSN_TIME_FORMAT);

  public FdsnCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }


  /**
   * Initiate response. Set variable and pass control to the command.
   * 
   * @param cmd
   * @param c
   * @param request
   */
  @Override
  public void respond(final String cmd, final SocketChannel c, final HttpRequest request) {
    this.socketChannel = c;
    this.request = request;
    this.cmd = cmd;
    arguments = request.getArguments();
    sendResponse();
  }

  protected void sendError(final int code, final String msg) {
    final StringBuilder sb = new StringBuilder();
    sb.append("Error " + code + ": " + HttpStatusCode.getReason(code) + "\n\n");
    sb.append(msg + "\n\n");
    sb.append("Usage details are available from http://" + request.getHeader("Host")
        + request.getFile() + "\n\n");
    sb.append("Request:\n");
    sb.append("http://" + request.getHeader("Host") + request.getFile() + "?"
        + request.getArgumentString() + "\n\n");
    sb.append("Request Submitted:\n");
    sb.append(Time.format(Time.FDSN_TIME_FORMAT, new Date(System.currentTimeMillis())) + "\n\n");
    sb.append("Service version:\n");
    sb.append(version);
    final String txt = sb.toString();
    final HttpResponse response = new HttpResponse("text/plain");
    response.setCode("" + code);
    response.setLength(txt.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(txt, socketChannel);
  }

}
