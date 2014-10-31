#!/bin/bash

# Helper to download all jdk 7 and 8 versions for testing
# For ref: https://gist.github.com/hgomez/4697585

curl -s http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html > jdk_list
curl -s http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html >> jdk_list
curl -s http://www.oracle.com/technetwork/java/java-archive-downloads-javase7-521261.html >> jdk_list
curl -s http://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase8-2177648.html >> jdk_list

cat jdk_list |
    grep -- -linux-x64.tar.gz |
    sed -r 's/.* = //;s/;$//' |
    while read line; do echo $line | jq -r .filepath ; done |
    grep jdk- |
    grep -- -b |
    sed 's#/otn/#/otn-pub/#' > sanitized_jdk_list

cat sanitized_jdk_list |
    while read uri; do
      printf "\n\n%s\n" $uri
      curl -L -b "oraclelicense=a" -O $uri
    done
