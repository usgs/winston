#!/bin/sh

PWD=`pwd`
ARGS=''

for arg in "$@" ; do
		case $1 in
	  	  -*) ARGS="$ARGS $1 $2" ; shift ;;
	  	  "") ;;
		   *) ARGS="$ARGS $PWD/$1" ;;
		esac
		
        shift
done

cd `dirname $0`/..
java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.ImportSeisan $ARGS
