#!/bin/sh
mvn install:install-file -Dfile=jcc-1.1.jar -DgroupId=javax.csapi.cc -DartifactId=jcc -Dversion=1.1 -Dpackaging=jar
mvn install:install-file -Dfile=javax-telephony-1.4.0 -DgroupId=javax.telephony -DartifactId=javax-telephony -Dversion=1.4.0 -Dpackaging=jar
mvn install:install-file -Dfile=jcat-0.3.1.jar -DgroupId=javax.jcat -DartifactId=jcat -Dversion=0.3.1 -Dpackaging=jar=jcat-0.3.1.jar -DgroupId=javax.jcat -DartifactId=jcat -Dversion=0.3.1 -Dpackaging=jar

mvn install:install-file -Dfile=asterisk-java-0.2.jar -DgroupId=net.sf.asteriskjava -DartifactId=asterisk-java -Dversion=0.2 -Dpackaging=jar
mvn install:install-file -Dfile=avaya-jtapi-7.1.0jar -DgroupId=com.avaya -DartifactId=jtapi -Dversion=7.1.0 -Dpackaging=jar
mvn install:install-file -Dfile=asterisk-jtapi-0.2.0.jar -DgroupId=org.asteriskjava -DartifactId=asterisk-jtapi -Dversion-0.2 -Dpackaging=jar
mvn install:install-file -Dfile=gjtapi-1.8.0.jar -DgroupId=gjtapi -DartifactId=gjtapi -Dpackaging=jar
