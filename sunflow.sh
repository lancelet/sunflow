#!/usr/bin/env sh
mem=1G
java -Xmx$mem -server -jar release/sunflow.jar $*
