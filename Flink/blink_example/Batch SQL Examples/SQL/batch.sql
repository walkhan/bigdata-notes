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