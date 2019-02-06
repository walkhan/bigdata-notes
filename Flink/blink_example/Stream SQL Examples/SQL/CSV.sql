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

