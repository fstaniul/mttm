#!/usr/bin/env bash
PIDFILE=./mttm.pid

start () {
    nohup java -jar mttm-1.0-SNAPSHOT.jar > /dev/null 2>&1 & echo $! > "$PIDFILE"
    echo "Started."
}

stop () {
    kill -SIGTERM $(cat "$PIDFILE")
    rm -r "$PIDFILE"
    echo "Stopped."
}

info () {
    echo "Accepted parameters are: start / stop / restart or no parameters then depending on existence of pid file will be started or stopped."
}

if [ "$#" == 0 ]; then
    if [ -f "$PIDFILE" ]; then
        stop
    else
        start
    fi
else

if [ "$1" == "start" ]
then
    if [ -f "$PIDFILE" ]
    then
        echo "Program is already running!"
    else
        start
    fi

elif [ "$1" == "stop" ]
then
    if [ -f "$PIDFILE" ]
    then
        stop
    else
        echo "Program is not running!"
    fi
elif [ "$1" == "restart" ]
then
    if [ -f "$PIDFILE" ]
    then
        stop
    fi

    start
else
    info
fi

fi