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
limit 10;


-- 重新触发某个任务，需要更新 expected_trigger_time，内部有幂等控制
update sx_sp_rt_task_instance
set status                = 0,
    enable                = 1,
    expected_trigger_time = 1646309825083,
    actual_trigger_time   = null,
    result=null
where id = '1499357922779353088'
limit 1;

-- 查询某个用户创建的任务
select *
from sx_sp_remind_task_info
where uid = '567cae72ae4cdf8cbcf6d9631dca1c85'
order by create_time desc
limit 3 \G;