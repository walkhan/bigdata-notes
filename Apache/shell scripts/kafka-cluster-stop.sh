#! /bin/bash

#kafka brokers hosts
brokers="h002 h003 h004"

#kafka classpath
KAFKA_HOME=/bigdata/kafka

#kafka shut down  info
echo "INFO:Begin to shut down kafka cluster......"

#kafka cluster begin to shut down
for broker in $brokers
do
 echo "INFO:shut down kafka cluster on ${broker}......"
  ssh $broker -C "source /etc/profile; ${KAFKA_HOME}/bin/kafka-server-stop.sh"
 if [ $? -eq 0 ];then
    echo "INFO:[${broker}] shut down successfully....."
 else
   echo "INFO:[${broker}] shut down Failure....."
 fi
done

#print kafka  complete  info
echo "INFO:kafka cluster shut down completed!"
