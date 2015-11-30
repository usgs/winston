package gov.usgs.volcanoes.winston.server.cmd.http.fdsn;

import gov.usgs.util.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author Tom Parker
 *
 */
public class FdsnDateFormat extends SimpleDateFormat {

    public static final String FDSN_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSS";
    
    public FdsnDateFormat() {
        super(Time.FDSN_TIME_FORMAT);
    }
    
    public Date parse(String source) throws ParseException {
        
        if (!source.contains("T"))
            source += "T00:00:00.0000";

        int t = source.indexOf('T');
        if (source.length() == t + 8)
            source += ".0000";
        
        if (source.length() > t + 13)
            source = source.substring(0, t+13);
        
        return super.parse(source);
    }
    
}
