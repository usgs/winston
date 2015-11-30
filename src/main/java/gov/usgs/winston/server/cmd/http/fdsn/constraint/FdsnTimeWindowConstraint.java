package gov.usgs.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.server.cmd.http.fdsn.FdsnException;

import java.text.ParseException;

/**
 * 
 * @author Tom Parker
 *
 */
public class FdsnTimeWindowConstraint extends FdsnTimeConstraint {

    double startBefore;
    double startAfter;
    double endBefore;
    double endAfter;

    public FdsnTimeWindowConstraint(String startBefore, String startAfter, String endBefore, String endAfter)
            throws FdsnException {
        try {
            this.startBefore = dateStringToDouble(startBefore, FAR_IN_FUTURE);
            this.startAfter = dateStringToDouble(startAfter, FAR_IN_PAST);
            this.endBefore = dateStringToDouble(endBefore, FAR_IN_FUTURE);
            this.endAfter = dateStringToDouble(endAfter, FAR_IN_PAST);
        } catch (ParseException e) {
            throw new FdsnException(400, "Can't parse time constraint: " + this);
        }

    }

    public boolean matches(Channel chan) {
        double start = chan.getMinTime();
        if (start > startBefore || start < startAfter)
            return false;
        
        double end = chan.getMaxTime();
        if (end > endBefore || end < endAfter)
            return false;
        
        return true;
    }

    public String toString() {
        return "FdsnTimeWindowConstraint: " + Util.j2KToDateString(startBefore) + " : " + Util.j2KToDateString(startAfter) + " : " + Util.j2KToDateString(endBefore) + " : " + Util.j2KToDateString(endAfter);
    }
}
