package tech.powerjob.common.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 分页对象
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class PageResult<T> implements Serializable {

    /**
     * 当前页数
     */
    private int index;
    /**
     * 页大小
     */
    private int pageSize;
    /**
     * 总页数
     */
    private int totalPages;
    /**
     * 总数据量
     */
    private long totalItems;
    /**
     * 数据
     */
    private List<T> data;

}
