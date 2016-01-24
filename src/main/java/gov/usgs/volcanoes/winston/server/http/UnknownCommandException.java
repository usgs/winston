/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http;

import java.io.IOException;

/**
 * Thrown when no class can handle the command.
 * 
 * @author Tom Parker
 *
 */
public class UnknownCommandException extends IOException {
  private static final long serialVersionUID = 1L;

}
