#!/usr/bin/env bash

set -e -u -o pipefail

mvn clean test-compile fmt:format -T 1.0C -Perror-prone -Perror-prone-fork -Ppatch -Pself-check -Derror-prone.patch-checks=ErrorProneTestHelperSourceFormat -Derror-prone.self-check-args='-XepOpt:ErrorProneTestHelperSourceFormat:AvoidTextBlocks=true -Xep:MethodReferenceUsage:OFF' -Dverification.skip
