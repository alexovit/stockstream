# Stock Stream
Example of Akka Stream client-server application to aggregate raw format stock data in one minute candlestick bars.

## Prerequisite
* Python (https://www.python.org/downloads/)
* SBT (https://www.scala-sbt.org/1.0/docs/Setup.html)

## Usage
* In a terminal launch an upstream server generating random stocks data
```
python upstream.py
```
* In another tab launch a downstream server that generates candlestick data in JSON format
```
sbt run
```
* Connect to a downstream server to get the results
```
netcat 127.0.0.1 6666
```
