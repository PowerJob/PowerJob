package tech.powerjob.server.common.options;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 通用选项，对应前端的下拉框组件
 *
 * @author tjq
 * @since 2024/8/24
 */
@Data
@Accessors(chain = true)
public class WebOption implements Serializable {

    /**
     * 选项值
     */
    private String code;
    /**
     * 选项暂时内容（默认中文，i18n 问题此处暂不考虑）
     */
    private String label;

    public static <T extends WebOptionAbility> List<WebOption> build(Class<T> enumClz) {
        List<WebOption> ret = Lists.newArrayList();
        T[] enumConstants = enumClz.getEnumConstants();
        for (T enumConstant : enumConstants) {
            WebOption webOption = new WebOption()
                    .setCode(enumConstant.getCode())
                    .setLabel(enumConstant.getLabel());
            ret.add(webOption);
        }
        return ret;
    }

}
