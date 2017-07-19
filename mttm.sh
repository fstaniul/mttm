#!/usr/bin/env bash
PIDFILE=./mttm.pid

process_exist () {
    [ ps -p $(cat "$PIDFILE") ]
}

start () {
    nohup java -jar mttm-1.1.jar > /dev/null 2>&1 & echo $! > "$PIDFILE"
    echo "Started."
}

stop () {
    if process_exist; then
        kill -SIGTERM $(cat "$PIDFILE")
        echo "Stopped."
    else
        echo "Program crushed. PID found but no process is running."
    fi

    rm -r "$PIDFILE"
}

status () {
    if [ -f "$PIDFILE" ]; then
        if process_exist; then
            echo "Program is running."
        else
            echo "Program crushed, PID found but no process is active. Deleting pid file."
            rm -r "$PIDFILE"
        fi
    else
        echo "Program is stopped."
    fi
}

info () {
    echo "Accepted parameters are: start / stop / restart / status or no parameters, then depending on existence of pid file will be started or stopped."
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

elif [ "$1" == "status" ]
then
    status

else
    info
fi

fi