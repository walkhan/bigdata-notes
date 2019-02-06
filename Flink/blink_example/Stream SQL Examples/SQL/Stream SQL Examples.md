# 一.本地集群
##   1.启动集群
```
./bin/start-cluster.sh 
```
##  2.CSV stream
### 2.1 数据准备
```
touch /tmp/input.csv
vim  /tmp/input.csv
hello
flink
hello
sql
hello
world
```
### 2.2 Flink SQL Client shell
```
./bin/sql-client.sh embedded
```
### 2.3 SQL脚本准备
```
create table csv_source (
  a varchar
) with (
  type = 'csv',
  path = 'file:///tmp/input.csv'
);

create table csv_sink (
  a varchar,
  c bigint
) with (
  type = 'csv',
  updatemode = 'upsert',
  path = 'file:///tmp/output.csv'
);
insert into csv_sink
select
  a,
  count(*)
from csv_source
group by a;
```

###  2.4. 查看output.csv文件
```
[root@fdw1 tmp]# cat output.csv 
Add,hello,1
Add,flink,1
Add,hello,2
Add,sql,1
Add,hello,3
Add,world,1
Add,spark,1
```
备注：修改sql-client-defaults.yaml中的execution:type: streaming


## 3.kafka-stream
### 3.1 启动SQL客户端
```
./bin/sql-client.sh embedded
```
  
### 3.2 输入SQL脚本
```
create table kafka_source (
  messageKey varbinary, 
  message varbinary, 
  topic varchar,
  partition int,
  offset bigint
) with (
  type = 'kafka010',   
  topic = 'action',
  bootstrap.servers = 'fdw1:9092,fdw3:9092,fdw4:9092',
  `group.id` = 'demo_group'
);


select
    date_format (visit_time, 'yyyy-MM-dd HH:mm') as `visit_time`,
    count (user_id) as pv,
    count (distinct user_id) as uv
from (
        select
            split_index (cast(message as varchar), ',', 0) as visit_time,
            split_index (cast(message as varchar), ',', 1) as user_id,
            split_index (cast(message as varchar), ',', 2) as visit_page,
            split_index (cast(message as varchar), ',', 3) as browser_type
        from
            kafka_source
    )
group by
    date_format (visit_time, 'yyyy-MM-dd HH:mm');
```
### 3.3.启动kafka生成者（打开另一个客户端）
```
   	./bin/kafka-console-producer.sh --topic action --broker-list fdw1:9092,fdw3:9092,fdw4:9092
	  1 >2018-10-16 09:00:00,1001,/page1,chrome
      2 >2018-10-16 09:00:02,1001,/page2,safari
      3 >2018-10-16 09:00:07,1005,/page1,safari
      4 >2018-10-16 09:01:30,1001,/page1,chrome
```
## 4.Java实现SQL查询

    




