package gov.usgs.winston.server.cmd.http.fdsn.event;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnWadlCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnEventWadl extends FdsnWadlCommand implements FdsnEventService{

    public FdsnEventWadl(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        template = "www/fdsnws/event_application.wadl";
        version = VERSION;
    }

    public void sendResponse() {
        sendError(501, "Winston does not support the Event service");
    }

    public String getCommand() {
        return "/fdsnws/event/1/application.wadl";
    }
}
