#!/bin/bash

startCommand='java -Xms1g -Xmx8g -jar nlpweb.jar --port 8040 --remotes remotes.conf'

demoPid=$(<nlpweb.pid)
if [ $? -eq 0 ]; then
    echo "killing previous instance: $demoPid"
    kill $demoPid
    echo "sleeping for 2 seconds"
    sleep 2
fi

echo "running start command: $startCommand"
nohup $startCommand > nlpweb.out 2> nlpweb.err &
ec=$?

sleep 2

ps -p $! > /dev/null
if [ $? -eq 0 ]; then
    echo $! > nlpweb.pid
    echo "pid: $!"
else
    echo "error: nlpweb failed to start!"
    exit 1
fi

exit $ec
