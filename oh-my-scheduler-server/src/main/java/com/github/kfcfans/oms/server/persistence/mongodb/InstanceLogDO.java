package com.github.kfcfans.oms.server.persistence.mongodb;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.util.List;

/**
 * 任务实例的运行时日志
 *
 * @author tjq
 * @since 2020/4/27
 */
@Data
@Document(collection = "instance_log")
public class InstanceLogDO {

    @Id
    private String id;

    private Long instanceId;

    private List<String> logList;
}
