goblin-console
==============
[![Build Status](https://travis-ci.org/aiyanbo/goblin-console.png?branch=master)](https://travis-ci.org/aiyanbo/goblin-console)

Goblin console, see https://github.com/aiyanbo/goblin-core

Requires
--------

- jdk1.7
- tomcat8.0
- systems: windows7, mac os x, linux

Build
------
- build jmotor-utility

```sh
git clone https://github.com/aiyanbo/jmotor-utility.git
cd jmotor-utility
mvn clean install
```

- build goblin-core

```sh
git clone https://github.com/aiyanbo/goblin-core.git
cd goblin-core
mvn clean install
```

- build goblin-console

```sh
git clone https://github.com/aiyanbo/goblin-console.git
cd goblin-console
mvn clean install
```

Install
-------
1. download tomcat8.0+
2. copy goblin-console.war to $TOMCAT_HOME/webapps
3. rename goblin-console.war to ROOT.war
4. go to $TOMCAT_HOME/bin
5. execute start script
