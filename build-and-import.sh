#!/usr/bin/env bash

mvn package -DskipTests

java -jar ./target/cwrum-event-tracing.jar ./test/cwrum_logs_tavler_rtk.json

echo
echo 'http://localhost:16686/search?end=1697752560000000&limit=1000&lookback=custom&maxDuration&minDuration=60s&service=cwrum&start=-2152314000000000&tags=%7B%22rum.userDetails.sessionId%22%3A%22bcc5b1e0-5dcc-4224-9381-33748ca1b8fa%22%7D'
