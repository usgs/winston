package gov.usgs.winston.server.cmd.http.fdsn.command;

import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

/**
 * 
 * @author Tom Parker
 *
 */
abstract public class FdsnVersionCommand extends FdsnCommand {

    protected FdsnVersionCommand(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
    }
    
    protected void sendResponse() {
        HttpResponse response = new HttpResponse("text/plain");
        response.setLength(version.length());
        netTools.writeString(response.getHeaderString(), socketChannel);
        netTools.writeString(version, socketChannel);
    }
}
