package tech.powerjob.server.initializer;

import lombok.Data;

import java.util.Map;

/**
 * 系统初始化上下文
 *
 * @author tjq
 * @since 2024/12/8
 */
@Data
public class SystemInitializerContext {

    private Long adminUserId;

    private Long defaultNamespaceId;

    private Map<String, Object> extra;
}
