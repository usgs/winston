package gov.usgs.winston.server.cmd.http.fdsn.event;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnQueryCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnEventQuery extends FdsnQueryCommand implements FdsnEventService {

    public FdsnEventQuery(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        version = VERSION;
    }

    public void sendResponse() {
        sendError(501, "Winston does not support the Event service");
    }
    
    public String getCommand() {
        return "/fdsnws/event/1/query";
    }
}
