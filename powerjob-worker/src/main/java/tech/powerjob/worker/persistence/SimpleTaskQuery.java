package tech.powerjob.worker.persistence;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 简单查询直接类，只支持 select * from task_info where xxx = xxx and xxx = xxx 的查询
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class SimpleTaskQuery {

    private static final String LINK = " and ";

    private String taskId;
    private Long subInstanceId;
    private Long instanceId;
    private String taskName;
    private String address;
    private Integer status;

    // 自定义的查询条件（where 后面的语句），如 crated_time > 10086 and status = 3
    private String queryCondition;
    // 自定义的查询条件，如 GROUP BY status
    private String otherCondition;

    // 查询内容，默认为 *
    private String queryContent = " * ";

    private Integer limit;

    public String getQueryCondition() {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(taskId)) {
            sb.append("task_id = '").append(taskId).append("'").append(LINK);
        }
        if (subInstanceId != null) {
            sb.append("sub_instance_id = ").append(subInstanceId).append(LINK);
        }
        if (instanceId != null) {
            sb.append("instance_id = ").append(instanceId).append(LINK);
        }
        if (!StringUtils.isEmpty(address)) {
            sb.append("address = '").append(address).append("'").append(LINK);
        }
        if (!StringUtils.isEmpty(taskName)) {
            sb.append("task_name = '").append(taskName).append("'").append(LINK);
        }
        if (status != null) {
            sb.append("status = ").append(status).append(LINK);
        }

        if (!StringUtils.isEmpty(queryCondition)) {
            sb.append(queryCondition).append(LINK);
        }

        String substring = sb.substring(0, sb.length() - LINK.length());

        if (!StringUtils.isEmpty(otherCondition)) {
            substring += otherCondition;
        }

        if (limit != null) {
            substring = substring + " limit " + limit;
        }
        return substring;
    }
}
