#!/bin/bash

echo "Install Asterisk AGI... "

JAR="mibs-asterisk-agi.jar"
CFG_FILE="application.properties"



SRC_CONFIG=$PWD"/src/main/resources/"
DST_CONFIG="/usr/local/etc/mibs-asterisk-agi/"


BUILD_DIR=$PWD"/build/libs/"

INSTALL_DIR="/usr/local/bin/"


SRC_CONFIG_FILE=$SRC_CONFIG$CFG_FILE
DST_CONFIG_FILE=$DST_CONFIG$CFG_FILE

DST_JAR=$INSTALL_DIR$JAR
SRC_JAR=$BUILD_DIR$JAR

ZER=10

copy_file(){ 

echo "Copy  $1 -> $2"	
cp $1 $2

if [ -e $2 ]
then
	echo "Copy sucessfull..."
else
	echo "Copy fail"
	exit 1
fi
}

if [ ! -e $DST_CONFIG  ]
then
	echo "Create directory "$DST_CONFIG
	mkdir $DST_CONFIG
fi
		

copy_file "$SRC_CONFIG_FILE" "$DST_CONFIG_FILE"
copy_file "$SRC_JAR" "$DST_JAR"



