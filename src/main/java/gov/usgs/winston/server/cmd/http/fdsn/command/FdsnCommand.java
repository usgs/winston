package gov.usgs.winston.server.cmd.http.fdsn.command;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.HttpStatusCode;
import gov.usgs.net.NetTools;
import gov.usgs.util.Time;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.AbstractHttpCommand;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnChannelConstraint;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnConstraint;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnGeographicCircleConstraint;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnGeographicSquareConstraint;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnTimeSimpleConstraint;
import gov.usgs.winston.server.cmd.http.fdsn.constraint.FdsnTimeWindowConstraint;

import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;

/**
 * 
 * @author Tom Parker
 * 
 *         TODO: fix POST argument handling
 * 
 */
abstract public class FdsnCommand extends AbstractHttpCommand {

    protected String version;

    protected static SimpleDateFormat dateFormat = new SimpleDateFormat(Time.FDSN_TIME_FORMAT);

    public FdsnCommand(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
    }


    /**
     * Initiate response. Set variable and pass control to the command.
     * 
     * @param cmd
     * @param c
     * @param request
     */
    public void respond(String cmd, SocketChannel c, HttpRequest request) {
        this.socketChannel = c;
        this.request = request;
        this.cmd = cmd;
        arguments = request.getArguments();
        sendResponse();
    }
    
    protected void sendError(int code, String msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error " + code + ": " + HttpStatusCode.getReason(code) + "\n\n");
        sb.append(msg + "\n\n");
        sb.append("Usage details are available from http://" + request.getHeader("Host") + request.getFile() + "\n\n");
        sb.append("Request:\n");
        sb.append("http://" + request.getHeader("Host") + request.getFile() + "?" + request.getArgumentString()
                + "\n\n");
        sb.append("Request Submitted:\n");
        sb.append(Time.format(Time.FDSN_TIME_FORMAT, new Date(System.currentTimeMillis())) + "\n\n");
        sb.append("Service version:\n");
        sb.append(version);
        String txt = sb.toString();
        HttpResponse response = new HttpResponse("text/plain");
        response.setCode("" + code);
        response.setLength(txt.length());
        netTools.writeString(response.getHeaderString(), socketChannel);
        netTools.writeString(txt, socketChannel);
    }

}
