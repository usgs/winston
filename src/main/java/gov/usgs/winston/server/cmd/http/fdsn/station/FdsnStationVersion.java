package gov.usgs.winston.server.cmd.http.fdsn.station;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnVersionCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnStationVersion extends FdsnVersionCommand implements FdsnStationService {

    public FdsnStationVersion(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        version = VERSION;
    }

    public String getCommand() {
        return "/fdsnws/station/1/version";
    }
}
