package gov.usgs.winston.server.cmd.http.fdsn.command;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 
 * @author Tom Parker
 * 
 */
abstract public class FdsnWadlCommand extends FdsnCommand {

    protected String template;
    
    public FdsnWadlCommand(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
    }

    protected void sendResponse() {
        HttpRequest req = new HttpRequest(cmd);

        InputStream in = this.getClass().getClassLoader().getResourceAsStream(template);
        StringBuilder inputStringBuilder = new StringBuilder();
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = bufferedReader.readLine();
            while(line != null){
                inputStringBuilder.append(line + "\n");
                line = bufferedReader.readLine();
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            
        }

        String xml = inputStringBuilder.toString();
        xml = xml.replace("%%HOST%%", req.getHeader("Host"));
        
        HttpResponse response = new HttpResponse("application/xml");
        response.setLength(xml.length());
        netTools.writeString(response.getHeaderString(), socketChannel);
        netTools.writeString(xml, socketChannel);
    }

    
//    public void sendStream(InputStream in, String mimeType, SocketChannel channel) {
//        try {
//            ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
//
//            byte[] tmp = new byte[in.available()];
//
//            int i = in.read(tmp);
//            while (i > 0) {
//                out.write(tmp, 0, i);
//                i = in.read(tmp);
//            }
//
//            byte[] bytes = out.toByteArray();
//            ByteBuffer buf = ByteBuffer.wrap(bytes);
//            HttpResponse response = new HttpResponse(mimeType);
//            response.setLength(bytes.length);
//            netTools.writeString(response.getHeaderString(), channel);
//            netTools.writeByteBuffer(buf, channel);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
