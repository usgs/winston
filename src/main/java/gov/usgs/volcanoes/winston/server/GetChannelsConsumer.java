package gov.usgs.volcanoes.winston.server;

import java.util.List;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

public class GetChannelsConsumer implements WinstonConsumer<List<Channel>> {
  
  boolean wantsMetadata;

  public GetChannelsConsumer() {
    this(false);
  }

  public GetChannelsConsumer(boolean wantsMetadata) {
    this.wantsMetadata = wantsMetadata;
  }
  
  @Override
  public List<Channel> execute(WinstonDatabase winston) throws UtilException {
    return new Channels(winston).getChannels(wantsMetadata);
  }
}
