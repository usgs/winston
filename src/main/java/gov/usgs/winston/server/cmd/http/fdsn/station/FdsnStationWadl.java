package gov.usgs.winston.server.cmd.http.fdsn.station;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnWadlCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnStationWadl extends FdsnWadlCommand implements FdsnStationService {

    public FdsnStationWadl(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        template = "www/fdsnws/station_application.wadl";
        version = VERSION;
    }

    public String getCommand() {
        return "/fdsnws/station/1/application.wadl";
    }
    
}
