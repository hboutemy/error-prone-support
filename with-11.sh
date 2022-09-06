#!/usr/bin/env bash

set -e -u -o pipefail

chjdk 11 && mvn clean install -Dversion.jdk.test=11 && mvn clean install -Perror-prone-fork -Pnon-maven-central -Pself-check -Dversion.jdk.test=11
