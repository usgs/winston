#!/bin/sh

cd `dirname $0`/..
java -cp lib/winston.jar gov.usgs.volcanoes.winston.server.WWS $*
