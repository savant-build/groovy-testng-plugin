#!/bin/bash
mkdir -p ~/.savant/cache/org/savantbuild/plugin/groovy-testng/0.1.0-\{integration\}/
cp build/jars/*.jar ~/.savant/cache/org/savantbuild/plugin/groovy-testng/0.1.0-\{integration\}/
cp src/main/resources/amd.xml ~/.savant/cache/org/savantbuild/plugin/groovy-testng/0.1.0-\{integration\}/groovy-testng-0.1.0-\{integration\}.jar.amd
cd ~/.savant/cache/org/savantbuild/plugin/groovy-testng/0.1.0-\{integration\}/
md5sum groovy-testng-0.1.0-\{integration\}.jar > groovy-testng-0.1.0-\{integration\}.jar.md5
md5sum groovy-testng-0.1.0-\{integration\}.jar.amd > groovy-testng-0.1.0-\{integration\}.jar.amd.md5
md5sum groovy-testng-0.1.0-\{integration\}-src.jar > groovy-testng-0.1.0-\{integration\}-src.jar.md5
