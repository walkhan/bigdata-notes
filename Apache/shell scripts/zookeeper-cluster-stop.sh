#! /bin/bash

#zookeeper hosts
zks="h002 h003 h004"

#zookeeper classpath
ZOOKEEPER_HOME=/bigdata/zookeeper

#zookeeper begin to shut down info
echo "INFO:Begin to shut down zookeeper cluster......"

#zookeeper cluster shut down
for zk in $zks
do
  echo "INFO:Start zookeeper on ${zk}......"
    ssh $zk -C "source /etc/profile; ${ZOOKEEPER_HOME}/bin/zkServer.sh stop"
  if [ $? -eq 0 ];then
    echo "INFO:[${zk}] shut down successfully......"
  fi
done

#print zookeeper shut down info
echo "INFO:Zookeeper cluster shut down completed!"
