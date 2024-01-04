package tech.powerjob.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * WorkerAppInfo
 *
 * @author tjq
 * @since 2023/9/2
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class WorkerAppInfo implements Serializable {

    /**
     * 应用唯一 ID
     */
    private Long appId;
}
