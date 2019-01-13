#! /bin/bash

#zookeeper hosts
zks="h002 h003 h004"

#zookeeper classpath
ZOOKEEPER_HOME=/bigdata/zookeeper

#zookeeper start info
echo "INFO:Zookeeper begin to start cluster......"

#zookeeper start cluster
for zk in $zks
do
  echo "INFO:Start zookeeper on ${zk}......"
    ssh $zk -C "source /etc/profile; ${ZOOKEEPER_HOME}/bin/zkServer.sh start"
  if [ $? -eq 0 ]; then
     echo "INFO:[${zk}] start successfully......"
  fi
done

#zookeeper cluster start success info
echo "INFO:Zookeeper cluster start successfully!"
