#! /bin/bash

#brokers host
brokers="h002 h003 h004"
#kafka classpath
KAFKA_HOME=/bigdata/kafka

# kafka start info
echo "INFO:Begin to start kafka cluster......"

# start kafka cluster
for broker in $brokers
do
  echo "INFO:Start kafka on ${broker}......"
  ssh $broker -C "source /etc/profile; sh ${KAFKA_HOME}/bin/kafka-server-start.sh -daemon ${KAFKA_HOME}/config/server.properties"
  if [ $? -eq 0 ]; then
      echo "INFO:[${broker}] Start successfully...... "
  else 
    echo "INFO:[${broker}] Start Failure......"
  fi
done

#print kafka  successfully info
echo "INFO:Kafka cluster start successfully!"
