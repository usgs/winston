/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

/**
 * Protocol independent base command.
 * 
 * @author Tom Parker
 *
 */
public abstract class BaseCommand {

  protected WinstonDatabasePool databasePool;

  protected BaseCommand() {}

  public void databasePool(WinstonDatabasePool databasePool) {
    this.databasePool = databasePool;
  }
}
