package tech.powerjob.official.processors.impl.script;

/**
 * python processor
 *
 * @author tjq
 * @since 2021/2/7
 */
public class PythonProcessor extends AbstractScriptProcessor {

    @Override
    protected String getScriptName(Long instanceId) {
        return String.format("python_%d.py", instanceId);
    }

    @Override
    protected String getRunCommand() {
        return "python";
    }
}
