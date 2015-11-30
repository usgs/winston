package gov.usgs.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.server.cmd.http.fdsn.FdsnException;

import java.text.ParseException;

/**
 * @author Tom Parker
 *
 */
public class FdsnTimeSimpleConstraint extends FdsnTimeConstraint {

    
    public final double startTime; // j2ksec
    public final double endTime; // j2ksec

    public FdsnTimeSimpleConstraint(String startTime, String endTime) throws FdsnException {
        try {
            this.startTime = dateStringToDouble(startTime, FAR_IN_PAST);
            this.endTime = dateStringToDouble(endTime, FAR_IN_FUTURE);
        } catch (ParseException e) {
            throw new FdsnException(400, "Can't parse time constraint: " + this);
        }
    }

    public boolean matches(Channel chan) {
        double end = chan.getMaxTime();
        if (end != Double.NaN && chan.getMaxTime() > endTime)
            return false;

        double start = chan.getMinTime();
        if (start != Double.NaN && chan.getMinTime() < startTime)
            return false;
        
        return true;
    }
    
    public boolean matches(TraceBuf buf) {
        double end = buf.lastSampleTime() + buf.samplingPeriod();
        if (end > endTime)
            return false;

        double start = buf.firstSampleTime();
        if (start < startTime)
            return false;
        
        return true;
    }

    public String toString() {
        return "FdsnTimeSimpleConstraint: " + Util.j2KToDateString(startTime) + ":" + Util.j2KToDateString(endTime);
    }


}
