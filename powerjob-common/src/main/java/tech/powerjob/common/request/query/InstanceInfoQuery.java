package tech.powerjob.common.request.query;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.common.PowerQuery;

@Getter
@Setter
@Accessors(chain = true)
public class InstanceInfoQuery extends PowerQuery {

    private Long jobIdEq;

}
