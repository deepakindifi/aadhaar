#!/bin/bash
kill $(ps aux | grep 'ReceiveRequest' | grep -v 'grep' | awk '{print $2}')
cd /home/ubuntu/aadhaar/;nohup java -cp .:../jars/* ReceiveRequest&

