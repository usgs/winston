package gov.usgs.volcanoes.winston.server.httpCmd;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeType {
  private static final Logger LOGGER = LoggerFactory.getLogger(MimeType.class);
    private static final Map<String, String> mimeTypes;
    
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("png", "image/png");
        map.put("jpg", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("gif", "image/gif");
        map.put("html", "text/html");
        map.put("ico", "image/x-icon");
        map.put("css", "text/css");
        map.put("js", "application/javascript");
        mimeTypes = Collections.unmodifiableMap(map);
    }
    
    public static String guessMimeType(String fileName) {
      LOGGER.info("guessing mime-type for {}", fileName);
        int index = fileName.indexOf('.');
        String extension = null;
        
        if (index == -1)
            extension = fileName;
        else if (fileName.length() + 2 > index)
            extension = fileName.substring(index+1);

        return mimeTypes.get(extension);
    }
}
