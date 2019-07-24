#!/bin/sh

DIR=`pwd`
cd `dirname $0`/..
java -cp lib/winston.jar gov.usgs.volcanoes.winston.db.Admin $*
cd $DIR
