package gov.usgs.volcanoes.winston.server.cmd.http;

import gov.usgs.net.HttpRequest;

public interface HttpBaseCommand {
    /**
     * Text used as anchor to navigate usage page
     * 
     * @return anchor text
     */
    abstract public String getAnchor();

    /**
     * Command title as displayed on usage page
     * 
     * @return command title
     */
    abstract public String getTitle();

    /**
     * Usage text to be included on usagpage. Embeded HTML is okay.
     * 
     * @return usage text
     */
    abstract public String getUsage(HttpRequest req);
}
