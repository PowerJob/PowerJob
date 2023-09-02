package tech.powerjob.worker.background.discovery;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 应用信息
 *
 * @author tjq
 * @since 2023/9/2
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class AppInfo {

    /**
     * 应用唯一 ID
     */
    private Long appId;
}
