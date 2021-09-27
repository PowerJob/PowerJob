package tech.powerjob.common.annotation;


import tech.powerjob.common.enums.CustomFeatureEnum;

import java.lang.annotation.*;

/**
 * @author Echo009
 * @since 2021/9/27
 *
 * 仅用于标记客制化的功能改动点，用于快速定位代码便于排查问题
 */
@Target({
        ElementType.TYPE,
        ElementType.ANNOTATION_TYPE,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.LOCAL_VARIABLE,
        ElementType.PARAMETER,
        ElementType.TYPE_PARAMETER,
        ElementType.TYPE_USE,
        ElementType.PACKAGE,
})
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface NetEaseCustomFeature {

    CustomFeatureEnum[] value();
    
}
