Kafka提供了一个kafka-topics.sh脚本工具对主题进行相关操作。它可以创建主题，删除主题，修改主题分区数和副本分配以及修改主题级别的配置。该脚本仅用一行核心代码

  `exec $(dirname $0)/kafka-run-class.sh kafka.admin.TopicCommand "$@"`
运行kafka-run-class.sh脚本调用kafka.admin.TopicCommand类，同时接收操作类指令，主令包括--list,--describe,--create,--alter --delete

## 创建主题 ##
创建主题有2种方式：
  
1. 如果代理设置了auto.create.topics.enable=true,该配置默认是true。如果生产者向为创建的主题发送消息时，会自动创建一个拥有${num.partitions}个分区和${default.replication.factor}个副本的主题。
2. 通过客户端执行kafka-topics.sh创建主题，创建主题的命令如下：

       bin/kafka-topics.sh \
       --create --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 \
       --replication-factor 3 \
       --partitions 3 \
       --topic test  
备注：   

 1. 副本数不能超过节点数，否则会创建失败。
 2. 创建主题，还可以通过config参数来设置主题级别的配置即覆盖默认主题，可以设置多级配置，具体格式如下：
    --config config1-name=config1-value --config config2-name=config2-value
 3. 创建主题可以设置该主题的max.message.bytes的字节，如404800，执行命令如下：
    --config max.message.bytes=404800

3. 登陆客户端查看所创建的主题的元数据信息
       bin/zkCli.sh -server cdh1:2181,cdh2:2181,cdh3:2181
       #查看分区
       ls /brokers/topics/actiom/partitions
       #查看分区复制因子
       get /brokers/topics/actiom

## 删除主题 ##
删除kafka主题有2种方式：
   
- 手动删除各节点${log.dir}目录下该主题分区文件夹，并登陆ZooKeeper客户端删除主题对应的节点，主题元素保存在/brokers/topics和/config/topics目录下。
- 执行kafka-topics.sh脚本进行删除，执行脚本的命令如下：
       bin/kafka-topics.sh \
       --delete  --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 \
       --topic test 

备注：如果要彻底删除主题，需要在配置中添加delete.topic.enable=true。默认是false，这种情况只是标记为删除状态。

## 查看主题 ##
kafka提供了list和describe两各命令查看主题的信息，其中list参数列出kafka所有的主题名，describe参数可以查看所有主题或者某个特定主题的信息
  1. 查看所有主题
         bin/kafka-topics.sh  --list \
         --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181
  
  2. 查看某个特定主题信息
         bin/kafka-topics.sh  --describe \
         --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181

  3. 查看正在同步的主题 
         bin/kafka-topics.sh  --describe \
         --under-replicated-partitions  \
         --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181 
         
   通过describe和under-replicated-partitions命令组合使用，可以查看处于“under replicated”状态的分区，处于该状态的主题可能正在进行同步操作。

  4.  查看没有Leader的分区
         bin/kafka-topics.sh  --describe \
         --unavailable-partitions \
         --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181
    
 通过describe和unavailable-partitions组合，可以查看没有Leader副本的主题，同时也可以指定topic参数。

  5. 查看主题覆盖的配置
         bin/kafka-topics.sh  --describe \
         --topics-with-overrides \
         --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181
  
   通过describe和topics-with-overrides组合，可以查看主题覆盖的配置。

## 修改主题 ##

1. 修改主题级别配置，使用kafka-topics.sh在后续版本中命令已经过期，推荐使用kafka-config.sh脚本。
2. 增加分区
   kafka不支持减少分区的操作，只能为一个分区添加分区，添加分区的命令如下：
         bin/kafka-topics.sh  --alter \
         --zookeeper 192.168.44.141:2181,192.168.44.131:2181,192.168.44.138:2181   \
         --topic action \
         --partitions 6 

## 生产者基本操作 ##
1.启动生产者

      bin/kafka-console-producer.sh 
      --broker-list 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
      --topic test \
      --property parse.key=true \
      --property key.separator=' ' 

如果需要改分隔符，则通过配置项 key.separator指定。parse.key指定每条消息包含key

2.查看某个主题各分区对应消息偏移量

    bin/kafka-run-class.sh kafka.tools.GetOffsetShell \
    --broker-list 192.168.44.141:9092,192.168.44.131:9092,192.168.44.138:9092 \
    --topic action \
    --time -1

可以通过partitions参数指定一个或者多个分区，多个分区之间可以逗号分隔，若不指定则默认查看该主题所有分区；time参数表示查看在指定时间之前的数据，支持-1(latest)，-2(earliest)，2个时间选项。通过以上命令可以看到主题名，分区编号以及生产的消息数。

3.查看生产者消息

    bin/kafka-run-class.sh kafka.tools.DumpLogSegments 
    --file /usr/local/kafka/kafkaLogs/actiom-0/00000000000000000000.log
file是必传参数，用于指定要转储（dump）文件的路径，可以同时指定多个文件，多个文件路径之间以逗号隔开。


备注：
   
 auto.leader.rebalance.enable=true    #提供自动平衡Leader分配的功能


