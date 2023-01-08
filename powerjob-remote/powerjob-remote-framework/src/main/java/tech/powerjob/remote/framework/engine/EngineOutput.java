package tech.powerjob.remote.framework.engine;

import lombok.Getter;
import lombok.Setter;
import tech.powerjob.remote.framework.transporter.Transporter;


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
