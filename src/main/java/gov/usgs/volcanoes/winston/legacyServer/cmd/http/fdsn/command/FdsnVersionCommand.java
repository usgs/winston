package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;

/**
 *
 * @author Tom Parker
 *
 */
abstract public class FdsnVersionCommand extends FdsnCommand {

  protected FdsnVersionCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {
    final HttpResponse response = new HttpResponse("text/plain");
    response.setLength(version.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(version, socketChannel);
  }
}
