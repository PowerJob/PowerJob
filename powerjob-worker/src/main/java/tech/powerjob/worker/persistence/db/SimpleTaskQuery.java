package tech.powerjob.worker.persistence.db;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.utils.CollectionUtils;

import java.util.Collection;
import java.util.stream.Collectors;

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

    private Collection<String> taskIds;

    private Long subInstanceId;
    private Long instanceId;
    private String taskName;
    private String address;
    private Integer status;

    // 查询内容，默认为 *
    private String queryContent = " * ";

    // 自定义的查询条件（where 后面的语句），如 crated_time > 10086 and status = 3
    private String queryCondition;
    // 自定义的查询条件，如 GROUP BY status
    private String otherCondition;

    private Integer limit;

    /**
     * 高级模式，完全自定义查询 SQL
     * 当传入此值时忽略 queryCondition + otherCondition + limit
     */
    private String fullCustomQueryCondition;

    /**
     * 是否设置为只读模式
     * 理论上全部查询均可设置，不过出于最小改动原则，仅针对新功能添加 readOnly
     */
    private boolean readOnly = false;

    public String getQueryCondition() {

        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(taskId)) {
            sb.append("task_id = '").append(taskId).append("'").append(LINK);
        }
        if (!CollectionUtils.isEmpty(taskIds)) {
            String taskIdsInQuery = taskIds.stream().map(id -> String.format("'%s'", id)).collect(Collectors.joining(", "));

            sb.append("task_id in (").append(taskIdsInQuery).append(")").append(LINK);
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

        // 自定义查询模式专用
        if (StringUtils.isNotEmpty(fullCustomQueryCondition)) {
            sb.append(fullCustomQueryCondition);
            return sb.toString();
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
