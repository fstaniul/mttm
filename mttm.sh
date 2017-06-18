#!/usr/bin/env bash
PIDFILE=./mttm.pid

start () {
    nohup java -jar mttm-1.0-SNAPSHOT.jar > ./log/$(date +%F).log 2>&1 & echo $! > "$PIDFILE"
}

killanddelete () {
    kill -9 $(cat "$PIDFILE")
    rm -d "$PIDFILE"
}

if ["$1" == "start"]
then
    if [ -f "$PIDFILE" ]
    then
        echo "Program is already running!"
    else
        start
    fi
fi

if ["$1" == "stop"]
then
    if [ -f "$PIDFILE" ]
    then
        killanddelete
    else
        echo "Program is not running!"
    fi
fi

if ["$1" == "restart"]
then
    if [ -f "$PIDFILE" ]
    then
        killanddelete
    fi

    start
fi