package tech.powerjob.official.processors.impl.script;

/**
 * shell processor
 *
 * @author tjq
 * @since 2021/2/7
 */
public class ShellProcessor extends AbstractScriptProcessor {

    @Override
    protected String getScriptName(Long instanceId) {
        return String.format("shell_%d.sh", instanceId);
    }

    @Override
    protected String getRunCommand() {
        return SH_SHELL;
    }
}
