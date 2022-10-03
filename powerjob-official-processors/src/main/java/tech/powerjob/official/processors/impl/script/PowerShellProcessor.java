package tech.powerjob.official.processors.impl.script;

import java.nio.charset.Charset;

/**
 * PowerShellProcessor
 *
 * @author fddc
 * @since 2021/5/14
 */
public class PowerShellProcessor extends AbstractScriptProcessor {

    @Override
    protected String getScriptName(Long instanceId) {
        return String.format("powershell_%d.ps1", instanceId);
    }

    @Override
    protected String getRunCommand() {
        return "powershell.exe";
    }

    @Override
    protected Charset getCharset() {
        return Charset.defaultCharset();
    }
}
