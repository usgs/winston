package gov.usgs.volcanoes.winston.server;

public abstract class BaseCommand {

  protected WinstonDatabasePool databasePool;

  protected BaseCommand() {}

  public void databasePool(WinstonDatabasePool databasePool) {
    this.databasePool = databasePool;
  }
}
