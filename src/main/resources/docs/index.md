# Using Winston

## Requirements
Winston relies on a number of external libraries which are included in the distribution package. The only other requirements are a Java JRE and a MySQL-compatible database.

- Java -- Any Java 7 compliant JVM should work. Both Oracle's [Java SE](http://www.java.com) and [OpenJDK](http://openjdk.java.net/) have been tested.

- Database -- Winston requires a MySQL-compatible database. Either [MySQL](http://www.mysql.com) or [MariaDB](https://mariadb.org) will work. The number of real-time stations Winston can handle is commonly limited by database write times. Winston avoids use of transactional features available in some MySQL storage engines to help streamline writes. Because of this, the MyISAM is the preferred storage engine for non-trivial Winston instalations.

## Instalation

1. Unzip the winston distribution in a convienient location.
1. Install MySQL or MariaDB
1. Create a winston user in the database.  
	<code>GRANT ALL ON \`W\_%\`.* to winstonuser@'localhost' identified by 'winstonpass';</code>

## Starting Winston
1. Ingest waveforms -- The Winston database is created automatically when the first waveforms are ingested. The wave server will not function until there is data to serve. Most winston users feed data from an [Earthworm](http://www.earthwormcentral.org) export. See the [ImportEW](ImportEW.md) page for the details. Data can also be loaded using [SEED](applications.md#importseed) or [SAC](applications.md#importsac) files.

1. Import station metadata (optional) -- Use [ImportDataless](applications.md#importdataless) or [ImportHypoinverse](applications.md#importhypoinverse) to populate station metadata tables.

1. Start wave server -- [Winston Wave Server](WWS.md) is used to serve data to Swarm and web browsers.
