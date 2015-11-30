Winston Readme

A manual for Winston is available online at:

http://www.avo.alaska.edu/Software/winston/W_Manual_TOC.html

If you have further questions about Winston please email Peter Cervelli,
pcervelli@usgs.gov.

Changelog:
2012-11-06:
	-- replaced seed-pdcc with JavaSeedLite to correct issue with data quality flag
	-- added config option limiting age of data that will be returned. Intended to be used when running multiple WWS instances pointing to a single database, each presenting a different retention policy.
2011-06-29: Winston 1.1.2 release
    -- added added administrative GUI
    -- several minor bug fixes
2010-02-26: Winston 1.1.1 release
	-- added rsam.enable configuration option to ImportEW
	-- added import.exportType configuration option to ImportEW
	-- added winston.StatementCacheCap to Winston.config
	-- fixed small memory leaks
	-- added new view to db schema (today UNION yesterday)
????-??-??: Winston 1.1 release
	?
2005-06-??: Winston 1.0.1 release
	-- added import.maxBacklog configuration option to ImportEW.
	-- fixed bug where ImportEW wasn't surviving database outages.
	-- added configurable log files to ImportEW.
	-- fixed exception when ImportEW started on headless system without '-n'.
	-- numerous small bug fixes.
	
2005-05-08: Initial Winston 1.0.0 release.