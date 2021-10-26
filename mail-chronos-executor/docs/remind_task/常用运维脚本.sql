-- 注意开始时间，当前实现下，会将开始时间创建的任务给算进去
select *,
       from_unixtime(next_trigger_time / 1000)             as next_trigger_time,
       from_unixtime((start_time + trigger_Offset) / 1000) as seed_time
from sx_sp_remind_task_info
where uid = 'e56d2b9c617bb68436aca452bc063e55'
  and recurrence_rule is not null
  and enable = 0
  and trigger_times != times_limit
order by create_time desc
limit 10 ;



