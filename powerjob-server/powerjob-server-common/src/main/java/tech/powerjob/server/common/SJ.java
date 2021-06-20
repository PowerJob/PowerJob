package tech.powerjob.server.common;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import tech.powerjob.common.OmsConstant;

/**
 * Splitter & Joiner
 *
 * @author tjq
 * @since 2020/5/27
 */
public class SJ {

    public static final Splitter COMMA_SPLITTER = Splitter.on(OmsConstant.COMMA);
    public static final Joiner COMMA_JOINER = Joiner.on(OmsConstant.COMMA);

}
