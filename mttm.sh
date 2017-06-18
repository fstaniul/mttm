#!/usr/bin/env bash

PIDFILE=./mttm.pid

if ["$1" == "start"]
then
    if [ -f "$PIDFILE" ]
    then
        echo "Program is already running!"
    else
        nohup java -jar mttm-1.0-SNAPSHOT.jar > ./log/$(date +%F).log 2>&1 & echo $! > "$PIDFILE"
    fi
fi

if ["$1" == "stop"]
then
    if [ -f "$PIDFILE" ]
    then
        kill -9 $(cat "$PIDFILE")
        rm -d "$PIDFILE"
    else
        echo "Program is not running!"
    fi
fi

if ["$1" == "restart"]
then
    if [ -f "$PIDFILE" ]
    then
        kill -9 $(cat "$PIDFILE")
        rm -d "$PIDFILE"
    fi

    nohup java -jar mttm-1.0-SNAPSHOT.jar > ./log/$(date +%F).log 2>&1 & echo $! > "$PIDFILE"
fi