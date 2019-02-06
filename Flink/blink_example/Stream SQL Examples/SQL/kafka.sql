create table kafka_source (
  messageKey varbinary, 
  message varbinary, 
  topic varchar,
  `partition` int,
  `offset` bigint
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
	
	
	
	./bin/kafka-console-producer.sh --topic action --broker-list fdw1:9092,fdw3:9092,fdw4:9092
	  1 >2018-10-16 09:00:00,1001,/page1,chrome
      2 >2018-10-16 09:00:02,1001,/page2,safari
      3 >2018-10-16 09:00:07,1005,/page1,safari
      4 >2018-10-16 09:01:30,1001,/page1,chrome