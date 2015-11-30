package gov.usgs.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.server.cmd.http.fdsn.FdsnDateFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnChannelConstraint implements FdsnConstraint {

    public final String network;
    public final String station;
    public final String channel;
    public final String location;

    /**
     * channel constraints submitted through a POST will also have a time
     * constraint.
     */
    private FdsnTimeSimpleConstraint timeConstraint;

    public FdsnChannelConstraint(String station, String channel, String network, String location) {

        this.station = stringToString(station, ".*");
        this.channel = stringToString(channel, ".*");
        this.network = stringToString(network, ".*");
        this.location = stringToString(location, ".*");

        if (this.station == null)
            System.out.println("is null");
        else if (this.station.equals("null"))
            System.out.println("is text null");
    }
    
    public void setTimeConstraint(FdsnTimeSimpleConstraint c) {
        timeConstraint = c;
    }

    private String stringToString(String in, String def) {
        if (in == null)
            return def;
        if ("".equals(in))
            return def;
        if ("null".equals(in))
            return def;

        return in;
    }

    public boolean matches(Channel chan) {

        if (chan == null)
            return false;

        String net = chan.network;
        if (net != null && !net.matches(network))
            return false;

        String cha = chan.channel;
        if (cha != null && !cha.matches(channel))
            return false;

        String sta = chan.station;
        if (sta != null && !sta.matches(station))
            return false;

        String loc = chan.location;
        if (loc != null && !loc.matches(location))
            return false;

        if (timeConstraint != null)
            return timeConstraint.matches(chan);
        
        return true;
    }

    public boolean matches(TraceBuf buf) {

        if (buf == null)
            return false;

        String net = buf.network();
        if (net != null && !net.matches(network))
            return false;

        String cha = buf.channel();
        if (cha != null && !cha.matches(channel))
            return false;

        String sta = buf.station();
        if (sta != null && !sta.matches(station))
            return false;

        String loc = buf.location();
        if (loc != null && !loc.matches(location))
            return false;

        if (timeConstraint != null)
            return timeConstraint.matches(buf);
        
        return true;        
    }

    public FdsnTimeSimpleConstraint getTimeConstraint() {
        return timeConstraint;
    }
    
    public String toString() {
        return "FdsnChannelConstraint: " + station + ":" + channel + ":" + network + ":" + location;
    }

}
