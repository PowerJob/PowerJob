package tech.powerjob.common.request.http;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.powerjob.common.request.common.RunJobRequest;

/**
 * RunJobOpenApiRequest
 *
 * @author tjq
 * @since 2024/8/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunJobOpenApiRequest extends RunJobRequest {
    /**
     * 自动填充 appId
     */
    private Long appId;
}
