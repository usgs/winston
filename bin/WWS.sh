#!/bin/sh

cd `dirname $0`/..
#java -XX:+HeapDumpOnOutOfMemoryError -cp lib/winston.jar gov.usgs.volcanoes.winston.server.WWS $*
java -cp lib/winston.jar gov.usgs.volcanoes.winston.server.WWS $*
