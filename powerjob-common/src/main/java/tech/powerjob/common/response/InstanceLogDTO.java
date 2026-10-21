package tech.powerjob.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * InstanceLog Network transmission object
 *
 * @author JasonXJL
 * @date 2024/12/13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceLogDTO {

    /**
     * 当前页数
     */
    private long index;
    /**
     * 总页数
     */
    private long totalPages;
    /**
     * 文本数据
     */
    private String data;
}
