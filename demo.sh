#!/bin/sh
# One command: compile, start both servers, send every example request with curl, stop the servers.
#   sh demo.sh
set -eu
cd "$(dirname "$0")"

mkdir -p out
javac -d out src/main/java/*.java                     # compile all .java files into out/

java -cp out Step2_BadServer & BAD=$!   # start the bad server (port 8080) in the background
java -cp out Step3_GoodServer & GOOD=$! # start the good server (port 8081) in the background
trap 'kill $BAD $GOOD 2>/dev/null || true' EXIT   # stop both servers when the script ends
sleep 1                                 # give the servers a moment to start

B=localhost:8080
G=localhost:8081

# call "title" <curl arguments...>
# prints a heading, then the full response (-i = with status line and headers)
call() {
  echo
  echo "=== $1"
  shift
  curl -sS -i "$@"
  echo
}

echo
echo "################  BAD API (port 8080): everything is 200  ################"
call "MS-101 (real room)"           "$B/api/reserve?ucid=10000001&room=MS-101"
call "NO-SUCH (failure, still 200)" "$B/api/reserve?ucid=10000001&room=NO-SUCH"

echo
echo "################  GOOD API (port 8081): the status tells the result  ################"
call "MS-101 (real room): 201 created R-1" -X POST "$G/v1/reservations" -d '{"ucid":"10000001","room":"MS-101"}'
call "NO-SUCH (failure): 400"              -X POST "$G/v1/reservations" -d '{"ucid":"10000001","room":"NO-SUCH"}'
call "200: read R-1 back"                  "$G/v1/reservations/R-1"
call "404: no such id"                     "$G/v1/reservations/R-missing"
call "v2: id instead of reservationId"     -X POST "$G/v2/reservations" -d '{"ucid":"10000001","room":"MS-101"}'
call "405: GET on the collection"          "$G/v1/reservations"
