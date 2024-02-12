package tech.powerjob.server.web.converter;

import org.springframework.beans.BeanUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.web.response.NamespaceVO;

/**
 * NamespaceConverter
 *
 * @author tjq
 * @since 2023/9/4
 */
public class NamespaceConverter {

    public static NamespaceVO do2BaseVo(NamespaceDO d) {
        NamespaceVO v = new NamespaceVO();
        BeanUtils.copyProperties(d, v);
        v.setGmtCreateStr(CommonUtils.formatTime(d.getGmtCreate()));
        v.setGmtModifiedStr(CommonUtils.formatTime(d.getGmtModified()));
        v.setStatusStr(SwitchableStatus.of(d.getStatus()).name());
        return v;
    }

}
