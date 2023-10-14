#/usr/bin
#cat zbus.sh | col -b > zbus.sh  ==> fix win=>lin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=./
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.mq.server.MqServer
MAIN_OPTS="-p 15555 -verbose false -store store -track"
LIB_OPTS="$ZBUS_HOME/lib/*:$ZBUS_HOME/classes:$ZBUS_HOME/*"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &


