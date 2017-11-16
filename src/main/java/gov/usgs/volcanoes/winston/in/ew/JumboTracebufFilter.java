package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;

/**
 *
 * @author Tom Parker
 */
public class JumboTracebufFilter extends TraceBufFilter {
  public JumboTracebufFilter() {
    super();
  }

  // todo: reimplement
  @Override
  public boolean match(final TraceBuf tb, final Options options) {
    return (false);
  }

  @Override
  public String toString() {
    return "JumboTracebufFilter";
  }
}
