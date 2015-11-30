package gov.usgs.winston.server.cmd.http.fdsn.dataselect;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnWadlCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnDataselectWadl extends FdsnWadlCommand implements FdsnDataselectService {

    public FdsnDataselectWadl(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        template = "www/fdsnws/dataselect_application.wadl";
    }

    public String getCommand() {
        return "/fdsnws/dataselect/1/application.wadl";
    }
}
