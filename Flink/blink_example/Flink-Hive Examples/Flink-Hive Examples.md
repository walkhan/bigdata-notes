在测试当中，发现连接hive是通过 hive.metastore.uris连接hive。所以要注意的 是hive-site.xml配置中必须含有此参数；第二个是从Flink目录下opt/connectors找到hive和hadoop的jar包拷贝到lib目录下。
## 一.本地集群
## 1.启动本地集群
```
./bin/start-cluster.sh
```
## 2,配置文件sql-client-defaults.yaml
```
execution:
  # 'batch' or 'streaming' execution
  type: batch
  # allow 'event-time' or only 'processing-time' in sources
  time-characteristic: event-time
  # interval in ms for emitting periodic watermarks
  periodic-watermarks-interval: 200
  # 'changelog' or 'table' presentation of results
  result-mode: table
  # maximum number of maintained rows in 'table' presentation of results
  max-table-result-rows: 1000000
  # parallelism of the program
  parallelism: 1
  # maximum parallelism
  max-parallelism: 4
  # minimum idle state retention in ms
  min-idle-state-retention: 0
  # maximum idle state retention in ms
  max-idle-state-retention: 0

deployment:
  # general cluster communication timeout in ms
  response-timeout: 5000
  # (optional) address from cluster to gateway
  gateway-address: ""
  # (optional) port from cluster to gateway
  gateway-port: 0

catalogs:
  - name: myhive
    catalog:
      type: hive
      connector:
        hive.metastore.uris: thrift://192.168.44.130:9083
```
## 3.数据准备
```
[root@fdw1 conf]# touch /tmp/data.txt
Tom,4.72
John,8.00
Tom,24.2
Bob,3.14
Bob,4.72
Tom,34.9
Mary,4.79
Tiff,2.72
Bill,4.33
Mary,77.7
```
## 4.打开hive
###   4.1创建表
```
hive> CREATE TABLE mytable(name string, value double) row format delimited fields terminated by ',' stored as textfile;
```
### 4.2.加载数据到hive  
```
hive> load data local inpath '/tmp/data.txt' into table mytable;
hive> select * from mytable;
OK
Tom	 4.72
John 8.0
Tom	 24.2
Bob	 3.14
Bob	 4.72
Tom	 34.9
Mary 4.79
Tiff 2.72
Bill 4.33
Mary 77.7
```
备注：关于hive的配置参数参考hive-site.xml
## 5.打开Flink SQL
### 5.1 启动客户端
```
./bin/sql-client.sh embedded
```

### 5.2.查询
```
Flink SQL>  show catalogs;
myhive
builtin
Flink SQL> use myhive.default;
Flink SQL> show tables;
mytable
Flink SQL> select * from mytable;
name                     value
Tom                      4.72
John                       8.0
Tom                      24.2
Bob                      3.14
Bob                      4.72
Tom                      34.9
Mary                      4.79
Tiff                      2.72
Bill                      4.33
Mary                      77.7
```
# 二.yarn session mode
## 1.启动yarn session 
```
./bin/yarn-session.sh -n 4 -s 4 -tm 2048 -nm yarnsession
```

## 2.配置sql-client-defaults.yaml
```
deployment:
  # general cluster communication timeout in ms
  response-timeout: 5000
  # (optional) address from cluster to gateway
  gateway-address: ""
  # (optional) port from cluster to gateway
  gateway-port: 0
  # (Optional) For users who use yarn-session mode
  yid: application_1548747363078_0001
```
备注：1.yarn session mode只需要将本地模式基础上添加yarn的appId
              2.其他操作和本地模式相同；
