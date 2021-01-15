package com.github.kfcfans.powerjob.common.request.http;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * JobQuery
 * eq: equals, =
 * gt: greater than, >
 * lt: less than, <
 *
 * @author tjq
 * @since 2021/1/15
 */
@Data
@Accessors(fluent = true, chain = true)
public class JobQuery {

    private Long appIdEq;

    private Long idEq;
    private Long idLt;
    private Long idGt;

    private List<Integer> statusIn;
    private List<Integer> statusNotIn;

    private Long nextTriggerTimeLt;
    private Long nextTriggerTimeGt;

    private String nameLike;

    private Long limit;
}
