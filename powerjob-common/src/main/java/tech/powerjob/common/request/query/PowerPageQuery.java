package tech.powerjob.common.request.query;

import lombok.Getter;
import lombok.Setter;
import tech.powerjob.common.PowerQuery;

import java.io.Serializable;

/**
 * 分页查询
 *
 * @author tjq
 * @since 2024/11/21
 */
@Getter
@Setter
public class PowerPageQuery extends PowerQuery implements Serializable {


    /* ****************** 分页参数  ****************** */
    /**
     * 当前页码
     */
    protected Integer index = 0;
    /**
     * 页大小
     */
    protected Integer pageSize = 10;

    /* ****************** 排序参数  ****************** */

    /**
     * 排序参数，如 gmtCreate、instanceId
     */
    protected String sortBy;

    /**
     * asc是指定列按升序排列，desc则是指定列按降序排列
     */
    protected boolean asc = false;
}
