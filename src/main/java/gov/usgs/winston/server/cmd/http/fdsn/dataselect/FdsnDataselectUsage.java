package gov.usgs.winston.server.cmd.http.fdsn.dataselect;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnUsageCommand;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnDataselectUsage extends FdsnUsageCommand implements FdsnDataselectService {

    public FdsnDataselectUsage(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        UrlBuillderTemplate = "www/fdsnws/dataselect_UrlBuilder";
        InterfaceDescriptionTemplate = "www/fdsnws/dataselect_InterfaceDescription";
    }

    public String getCommand() {
        return "/fdsnws/dataselect/1";
    }
}
