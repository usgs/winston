package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.dataselect;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command.FdsnWadlCommand;

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
