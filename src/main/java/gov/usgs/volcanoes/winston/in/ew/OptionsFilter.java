package gov.usgs.volcanoes.winston.in.ew;

import java.util.ArrayList;
import java.util.List;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.configfile.ConfigFile;

/**
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.2 2006/04/01 23:43:49 cervelli
 * Clean up.
 *
 * @author Dan Cervelli
 */
public class OptionsFilter {
  private String name;
  private Options options;
  private List<SCNLFilter> filters;

  public OptionsFilter(final String n, final ConfigFile cf, final Options def) {
    name = n;
    options = Options.createOptions(cf, def);

    final String at = cf.getString("applyTo");
    if (at == null)
      return;

    final String[] fs = at.split(",");
    filters = new ArrayList<SCNLFilter>(fs.length);
    for (final String f : fs) {
      final SCNLFilter filter = new SCNLFilter(f.trim());
      filters.add(filter);
    }
  }

  public String getName() {
    return name;
  }

  public boolean match(final TraceBuf tb) {
    for (final SCNLFilter filter : filters) {
      if (filter.match(tb, null))
        return true;
    }
    return false;
  }

  public Options getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return options.toString();
  }

}
