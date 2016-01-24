package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command;

import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import gov.usgs.net.HttpRequest;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.FdsnException;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint.FdsnChannelConstraint;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint.FdsnConstraint;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint.FdsnGeographicCircleConstraint;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint.FdsnGeographicSquareConstraint;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint.FdsnTimeSimpleConstraint;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint.FdsnTimeWindowConstraint;

/**
 *
 * @author Tom Parker
 *
 */
abstract public class FdsnQueryCommand extends FdsnCommand {
  protected static final Channel[] DUMMY_CHANNEL_ARRAY = new Channel[0];

  protected Deque<FdsnChannelConstraint> channelConstraints;
  protected FdsnConstraint geographicConstraint;
  protected FdsnConstraint timeConstraint;
  protected Channel[] chanList;
  protected Channels channels;
  protected Channel[] prunedChanList;

  protected FdsnQueryCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    channels = new Channels(db);
  }


  /**
   * Initiate response. Set variable and pass control to the command.
   * 
   * @param cmd
   * @param c
   * @param request
   */
  @Override
  public void respond(final String cmd, final SocketChannel c, final HttpRequest request) {
    this.socketChannel = c;
    this.request = request;
    this.cmd = cmd;
    arguments = request.getArguments();
    try {
      parseArguments();
      final List<Channel> chans = channels.getChannels();

      if (chans == null) {
        sendError(404, "Channels to found.");
        return;
      }

      chanList = channels.getChannels().toArray(DUMMY_CHANNEL_ARRAY);
      prunedChanList = pruneChanList();
      Arrays.sort(prunedChanList);
      sendResponse();
    } catch (final FdsnException e) {
      sendError(e.code, e.message);
    }
  }

  /**
   * fdsn-ws uses a goofy format for POST requests. We need to parse the
   * arguments again.
   */
  protected void parseArguments() throws FdsnException {

    channelConstraints = new ArrayDeque<FdsnChannelConstraint>();
    if ("POST".equals(request.getMethod()))
      parsePost();
    else
      parseGet();
  }

  protected void parseGet() throws FdsnException {
    getChannelConstraints();
    getTimeConstraint();
    getGeographicConstraint();
  }


  private void getChannelConstraints() {
    final String[] nets = getArgs(arguments.get("net") + "," + arguments.get("network"));
    final String[] stas = getArgs(arguments.get("sta") + "," + arguments.get("station"));
    final String[] locs = getArgs(arguments.get("loc") + "," + arguments.get("location"));
    final String[] chas = getArgs(arguments.get("cha") + "," + arguments.get("channel"));

    // The 1.1 spec doesn't specify how to combine the channel constraints.
    // I'm assuming a Cartesian product.
    for (final String net : nets) {
      for (final String sta : stas) {
        for (String loc : locs) {
          for (final String cha : chas) {
            if (loc.equals("  "))
              loc = "--";

            FdsnChannelConstraint con;
            con = new FdsnChannelConstraint(sta, cha, net, loc);
            channelConstraints.add(con);
            System.out.println(con);
          }
        }
      }
    }

  }


  private void getGeographicConstraint() throws FdsnException {

    final String latitude = getArg("latitude", "lat");
    final String longitude = getArg("longitude", "lon");
    final String minRadius = arguments.get("minradius");
    final String maxRadius = arguments.get("maxradius");
    if (!(latitude == null && longitude == null && minRadius == null && maxRadius == null)) {
      geographicConstraint =
          new FdsnGeographicCircleConstraint(latitude, longitude, minRadius, maxRadius);
    } else {
      final String minLatitude = getArg("minlatitude", "minLat");
      final String maxLatitude = getArg("maxlatitude", "maxLat");
      final String minLongitude = getArg("minlongitude", "minLon");
      final String maxLongitude = getArg("maxlongitude", "maxLon");

      geographicConstraint =
          new FdsnGeographicSquareConstraint(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    System.out.println(geographicConstraint);
  }


  private void getTimeConstraint() throws FdsnException {
    final String startBefore = arguments.get("startbefore");
    final String startAfter = arguments.get("startafter");
    final String endBefore = arguments.get("endbefore");
    final String endAfter = arguments.get("andafter");
    if (startBefore != null || startAfter != null || endBefore != null || endAfter != null) {
      timeConstraint = new FdsnTimeWindowConstraint(startBefore, startAfter, endBefore, endAfter);
    } else {
      final String startTime = getArg("startTime", "start");
      final String endTime = getArg("endTime", "end");

      timeConstraint = new FdsnTimeSimpleConstraint(startTime, endTime);
    }

    System.out.println(timeConstraint);
  }


  private String getArg(final String s1, final String s2) {
    String arg = arguments.get(s1);
    if (arg == null)
      arg = arguments.get(s2);

    return arg;
  }

  protected String[] getArgs(String arg) {
    if (arg == null || arg.equals("") || arg.equals("null"))
      return new String[0];

    // trim null args
    arg = arg.replace(",null", ",");
    arg = arg.replace("null,", ",");
    arg = arg.replaceFirst("^,", "");
    arg = arg.replaceFirst(",$", "");
    arg = arg.replace(",,", ",");

    // convert wildcards to regular expressions
    arg = arg.replace("*", ".*");
    arg = arg.replace("?", ".?");

    return arg.split(",");
  }

  protected void parsePost() throws FdsnException {
    final String[] lines = request.fullRequest.split("\r\n");

    // find first line of POST body
    int index = 0;
    for (final String line : lines) {
      index++;
      if ("".equals(line))
        break;
    }

    while (index < lines.length) {
      String[] s = lines[index].split("=");
      if (s.length == 2) {
        arguments.put(s[0], s[1]);
      } else {
        s = lines[index].split(" ");
        if (lines.length != 6) {
          throw new FdsnException(400, "Can't parse SCNL request " + lines[index]);
        } else {
          if (s[2].equals("  "))
            s[2] = "--";

          FdsnChannelConstraint c;
          c = new FdsnChannelConstraint(s[1], s[3], s[0], s[2]);
          channelConstraints.add(c);
          c.setTimeConstraint(new FdsnTimeSimpleConstraint(s[4], s[5]));
        }
      }
    }
  }

  protected Channel[] pruneChanList() {
    final Deque<Channel> prunnedChans = new ArrayDeque<Channel>();

    for (final Channel c : chanList)
      if (checkChannel(c))
        prunnedChans.offer(c);

    return prunnedChans.toArray(DUMMY_CHANNEL_ARRAY);
  }

  private boolean checkChannel(final Channel c) {
    boolean matches = false;
    for (final FdsnConstraint cons : channelConstraints) {
      if (cons.matches(c)) {
        matches = true;
        break;
      }
    }

    if (matches && timeConstraint != null && !timeConstraint.matches(c))
      matches = false;

    if (matches && geographicConstraint != null && !geographicConstraint.matches(c))
      matches = false;

    return matches;
  }

}
