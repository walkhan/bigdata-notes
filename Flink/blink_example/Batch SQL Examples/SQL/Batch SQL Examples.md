# 一.本地集群
##   1.启动集群
```
./bin/start-cluster.sh 
```
##  2.准备数据
```
touch /tmp/pagevisit.csv
vim  /tmp/pagevisit.csv
2018-10-16 09:00:00,1001,/page1,chrome
2018-10-16 09:00:20,1001,/page2,safari
2018-10-16 09:03:20,1005,/page1,chrome
2018-10-16 09:05:50,1005,/page1,safari
2018-10-16 09:05:56,1005,/page2,safari
2018-10-16 09:05:57,1006,/page2,chrome
```
## 3.启动Flink SQL客户端  
```
./bin/sql-client.sh embedded
```
### 3.1.执行SQL脚本
```
create table pagevisit (
    visit_time varchar,
    user_id bigint,
    visit_page varchar,
    browser_type varchar
) with (
    type = 'csv',
    path = 'file:///tmp/pagevisit.csv'
);

select 
  date_format(visit_time, 'yyyy-MM-dd HH:mm') as `visit_time`,
  count(user_id) as pv, 
  count(distinct user_id) as uv
from pagevisit
group by date_format(visit_time, 'yyyy-MM-dd HH:mm');
```

### 3.2.结果
```
 visit_time                         pv                        uv
  2018-10-16 09:00                  2                         1
  2018-10-16 09:03                  1                         1
  2018-10-16 09:05                  3                         2
```
## 4.Java实现SQL查询

