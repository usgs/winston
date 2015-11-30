package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 *
 * @author Tom Parker
 *
 */
abstract public class FdsnWadlCommand extends FdsnCommand {

  protected String template;

  public FdsnWadlCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  @Override
  protected void sendResponse() {
    final HttpRequest req = new HttpRequest(cmd);

    final InputStream in = this.getClass().getClassLoader().getResourceAsStream(template);
    final StringBuilder inputStringBuilder = new StringBuilder();
    BufferedReader bufferedReader;
    try {
      bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      String line = bufferedReader.readLine();
      while (line != null) {
        inputStringBuilder.append(line + "\n");
        line = bufferedReader.readLine();
      }
    } catch (final UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IOException e) {

    }

    String xml = inputStringBuilder.toString();
    xml = xml.replace("%%HOST%%", req.getHeader("Host"));

    final HttpResponse response = new HttpResponse("application/xml");
    response.setLength(xml.length());
    netTools.writeString(response.getHeaderString(), socketChannel);
    netTools.writeString(xml, socketChannel);
  }


  // public void sendStream(InputStream in, String mimeType, SocketChannel channel) {
  // try {
  // ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
  //
  // byte[] tmp = new byte[in.available()];
  //
  // int i = in.read(tmp);
  // while (i > 0) {
  // out.write(tmp, 0, i);
  // i = in.read(tmp);
  // }
  //
  // byte[] bytes = out.toByteArray();
  // ByteBuffer buf = ByteBuffer.wrap(bytes);
  // HttpResponse response = new HttpResponse(mimeType);
  // response.setLength(bytes.length);
  // netTools.writeString(response.getHeaderString(), channel);
  // netTools.writeByteBuffer(buf, channel);
  // } catch (IOException e) {
  // e.printStackTrace();
  // }
  // }
}
