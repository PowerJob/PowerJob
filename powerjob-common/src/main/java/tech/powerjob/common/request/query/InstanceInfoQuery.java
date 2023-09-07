package tech.powerjob.common.request.query;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.common.PowerQuery;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class InstanceInfoQuery extends PowerQuery {
    private Long idEq;
    private Long idLt;
    private Long idGt;

    private Long jobIdEq;
    private Long jobIdLt;
    private Long jobIdGt;

    private Long instanceIdEq;
    private Long instanceIdLt;
    private Long instanceIdGt;

    private String jobParamsEq;
    private String jobParamsLike;

    private String instanceParamsEq;
    private String instanceParamsLike;

    private List<Integer> typeIn;

    private Long wfInstanceIdEq;

    private List<Integer> statusIn;

    private String resultEq;
    private String resultLike;
    private String resultNotLike;

    private Long expectedTriggerTimeGt;
    private Long expectedTriggerTimeLt;

    private Long actualTriggerTimeGt;
    private Long actualTriggerTimeLt;

    private Long finishedTimeGt;
    private Long finishedTimeLt;

    private Date gmtCreateLt;
    private Date gmtCreateGt;

    private Date gmtModifiedLt;
    private Date gmtModifiedGt;
}
