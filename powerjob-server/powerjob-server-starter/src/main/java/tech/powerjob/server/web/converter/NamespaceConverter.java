package tech.powerjob.server.web.converter;

import org.springframework.beans.BeanUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.web.response.NamespaceBaseVO;

/**
 * NamespaceConverter
 *
 * @author tjq
 * @since 2023/9/4
 */
public class NamespaceConverter {

    public static NamespaceBaseVO do2BaseVo(NamespaceDO d) {
        NamespaceBaseVO v = new NamespaceBaseVO();

        BeanUtils.copyProperties(d, v);

        v.setGmtCreateStr(CommonUtils.formatTime(d.getGmtCreate()));
        v.setGmtModifiedStr(CommonUtils.formatTime(d.getGmtModified()));
        v.setStatusStr(SwitchableStatus.of(d.getStatus()).name());

        v.genShowName();
        return v;
    }

}
