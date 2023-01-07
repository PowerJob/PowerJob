package tech.powerjob.remote.framework.engine;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.util.Map;

/**
 * 引擎输出
 *
 * @author tjq
 * @since 2022/12/31
 */
@Getter
@Setter
public class EngineOutput {
    private Transporter transporter;
}
