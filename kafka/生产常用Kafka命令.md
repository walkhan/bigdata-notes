### 1.创建主题
```
       bin/kafka-topics.sh \
       --create 
       --zookeeper           192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 \
       --replication-factor 3 \
       --partitions 3 \
       --topic test  
```
如果是单节点本地测试，zookeeper可以采用localhost:2181访问

### 2.查看主题
 查看所有主题
```
bin/kafka-topics.sh  --list \
--zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181
```
  
 查看某个特定主题信息
```
bin/kafka-topics.sh  --describe \
--zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181
```

 查看正在同步的主题 
```
bin/kafka-topics.sh  --describe \
  --under-replicated-partitions  \
  --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181
```

如果是单节点本地测试，zookeeper可以采用localhost:2181访问

### 3.修改主题
```
bin/kafka-topics.sh  --alter \
  --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181   \
  --topic action \
  --partitions 6 
```
如果是单节点本地测试，zookeeper可以采用localhost:2181访问

### 4.删除主题
```
 bin/kafka-topics.sh \
 --delete  \
 --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 \
 --topic test 
```
如果是单节点本地测试，zookeeper可以采用localhost:2181访问

### 5.启动生产者
```
bin/kafka-console-producer.sh 
 --broker-list 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
 --topic test \
 --sync \
 --productor.config /file_path/client.properties
```
需要注意的是如果启用了kerberos认证，需要添加--productor.config执行才可以。

### 6.启动消费者
```
bin/kafka-console-consumer.sh \
 --bootstrap-server 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
 --topic test \
 --from-beginning \
 --consumer-property group.id= test_id \
 --consumer-property client.id= test_id \
 --consumer.config /file_path/client.properties
```
需要注意的是如果启用了kerberos认证，需要添加--consumer.config执行才可以。

### 7.查看当前组的消费进度
```
bin/kafka-consumer-groups.sh \
  --bootstrap-server 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
  --group test_id \
  --describe \
  --command-config  /file_path/client.properties
```

说明：分区LAG列的值如果都为0，表示数据全部消费完毕。

### 8.分区移位被重设到0
```
 bin/kafka-consumer-groups.sh \
   --bootstrap-server 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
   --group test_id \
   --reset-offsets \
   -all-earliest \
   --execute \
   --command-config  /file_path/client.properties
```

### 9.查看副本列表
```
 bin/kafka-topics.sh \
   --describe  
   --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 \
   --topic test 
```
以上命令在有Kerberos认证的情况下，需要在执行命令前，执行环境变量生效
export KAFKA_OPTS="-Djava.security.auth.login.config=/file_path/jaas-keytab.conf"

### 10.查看topic的消息总数
```
bin/kafka-run-class.sh \
 kafka.tools.GetOffsetShell \
 --broker-list 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
 --topic test \
 --time -1
```
 --time: -1表示消息的最大位移； --time: -2表示消息的当前位移。如果要计算消费者消费的消息总数，计算公式为：分区数 * ((--time -1) - (--time -2)) ;
### 11.检查消费者消费了多少消息
```
bin/kafka-consumer-offset-checker.sh \
kafka.tools.ConsumerOffsetChecker \
--group test_id \
--zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 \
--topic test
```
参数介绍：logsize,消息总数；offset,已经消费的条数；lag,还为消费的条数
