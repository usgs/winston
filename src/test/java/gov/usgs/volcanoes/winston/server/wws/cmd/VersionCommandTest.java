package gov.usgs.volcanoes.winston.server.wws.cmd;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

public class VersionCommandTest {

  @Mock
  ChannelHandlerContext ctx;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule(); 
  
  @Test
  public void sends_version_string() throws MalformedCommandException  {
    String req = "Version: GS";
    String expected = "PROTOCOL_VERSION: " + VersionCommand.PROTOCOL_VERSION + "\n";
    VersionCommand  cmd = new VersionCommand();
    cmd.doCommand(ctx, new WwsCommandString(req));
    Mockito.verify(ctx).write(expected);
  }
}
