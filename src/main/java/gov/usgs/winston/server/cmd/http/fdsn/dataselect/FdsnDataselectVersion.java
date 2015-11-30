package gov.usgs.winston.server.cmd.http.fdsn.dataselect;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnVersionCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnDataselectVersion extends FdsnVersionCommand implements FdsnDataselectService {

    public FdsnDataselectVersion(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        version = VERSION;
    }

    public String getCommand() {
        return "/fdsnws/dataselect/1/version";
    }
}
