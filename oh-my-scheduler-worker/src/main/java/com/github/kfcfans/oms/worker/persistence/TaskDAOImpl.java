package com.github.kfcfans.oms.worker.persistence;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 任务持久化实现层，表名：task_info
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class TaskDAOImpl implements TaskDAO {

    @Override
    public boolean initTable() {

        String delTableSQL = "drop table if exists task_info";
        String createTableSQL = "create table task_info (task_id varchar(20), instance_id varchar(20), job_id varchar(20), task_name varchar(20), task_content text, address varchar(20), status int(11), result text, created_time bigint(20), last_modified_time bigint(20), unique key pkey (instance_id, task_id))";

        try (Connection conn = ConnectionFactory.getConnection(); Statement stat = conn.createStatement()) {
            stat.execute(delTableSQL);
            stat.execute(createTableSQL);
        }catch (Exception e) {
            log.error("[TaskDAO] initTable failed.", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean save(TaskDO task) {
        String insertSQL = "insert into task_info(task_id, instance_id, job_id, task_name, task_content, address, status, result, created_time, last_modified_time) values (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSQL)) {
            fillInsertPreparedStatement(task, ps);
            return ps.execute();
        }catch (Exception e) {
            log.error("[TaskDAO] insert failed.", e);
        }
        return false;
    }

    @Override
    public boolean batchSave(Collection<TaskDO> tasks) {
        String insertSQL = "insert into task_info(task_id, instance_id, job_id, task_name, task_content, address, status, result, created_time, last_modified_time) values (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSQL)) {

            for (TaskDO task : tasks) {

                fillInsertPreparedStatement(task, ps);
                ps.addBatch();
            }

            ps.executeBatch();
            return true;

        }catch (Exception e) {
            log.error("[TaskDAO] insert failed.", e);
        }
        return false;
    }


    @Override
    public boolean update(TaskDO task) {
        return false;
    }

    @Override
    public TaskDO selectByKey(String instanceId, String taskId) {
        String selectSQL = "select * from task_info where instance_id = ? and task_id = ?";
        ResultSet rs = null;
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement ps = conn.prepareStatement(selectSQL)) {
            ps.setString(1, instanceId);
            ps.setString(2, taskId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return convert(rs);
            }
        }catch (Exception e) {
            log.error("[TaskDAO] selectByKey failed(instanceId = {}, taskId = {}).", instanceId, taskId, e);
        }finally {
            if (rs != null) {
                try {
                    rs.close();
                }catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    @Override
    public List<TaskDO> simpleQuery(SimpleTaskQuery query) {

        ResultSet rs = null;
        String sql = query.getQuerySQL();
        List<TaskDO> result = Lists.newLinkedList();
        try (Connection conn = ConnectionFactory.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(convert(rs));
            }
        }catch (Exception e) {
            log.error("[TaskDAO] simpleQuery failed(sql = {}).", sql, e);
        }finally {
            if (rs != null) {
                try {
                    rs.close();
                }catch (Exception ignore) {
                }
            }
        }
        return result;
    }

    private static TaskDO convert(ResultSet rs) throws SQLException {
        TaskDO task = new TaskDO();
        task.setTaskId(rs.getString("task_id"));
        task.setInstanceId(rs.getString("instance_id"));
        task.setJobId(rs.getString("job_id"));
        task.setTaskName(rs.getString("task_name"));
        task.setTaskContent(rs.getString("task_content"));
        task.setAddress(rs.getString("address"));
        task.setStatus(rs.getInt("status"));
        task.setResult(rs.getString("result"));
        task.setCreatedTime(rs.getLong("created_time"));
        task.setLastModifiedTime(rs.getLong("last_modified_time"));
        return task;
    }

    private static void fillInsertPreparedStatement(TaskDO task, PreparedStatement ps) throws SQLException {
        ps.setString(1, task.getTaskId());
        ps.setString(2, task.getInstanceId());
        ps.setString(3, task.getJobId());
        ps.setString(4, task.getTaskName());
        ps.setString(5, task.getTaskContent());
        ps.setString(6, task.getAddress());
        ps.setInt(7, task.getStatus());
        ps.setString(8, task.getResult());
        ps.setLong(9, task.getCreatedTime());
        ps.setLong(10, task.getLastModifiedTime());
    }

    public static void main(String[] args) throws Exception {
        TaskDAOImpl taskDAO = new TaskDAOImpl();
        taskDAO.initTable();

        TaskDO taskDO = new TaskDO();
        taskDO.setJobId("11");
        taskDO.setInstanceId("22");
        taskDO.setTaskId("2.1");
        taskDO.setTaskName("zzz");
        taskDO.setTaskContent("hhhh");

        taskDAO.save(taskDO);

        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId("22");
        query.setTaskId("2.1");
        System.out.println(taskDAO.simpleQuery(query));

        Thread.sleep(100000);
    }
}
