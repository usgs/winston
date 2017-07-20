package gov.usgs.volcanoes.winston.client;

import java.io.IOException;

public interface WWSCommandHandler {
	public void handle(Object msg) throws IOException;
}
