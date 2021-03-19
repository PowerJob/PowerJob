package tech.powerjob.worker.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 打印启动 Banner
 *
 * @author tjq
 * @since 2020/5/11
 */
@Slf4j
public final class PowerBannerPrinter {

    private static final String BANNER = "" +
            "\n" +
            " ███████                                          ██          ██\n" +
            "░██░░░░██                                        ░██         ░██\n" +
            "░██   ░██  ██████  ███     ██  █████  ██████     ░██  ██████ ░██\n" +
            "░███████  ██░░░░██░░██  █ ░██ ██░░░██░░██░░█     ░██ ██░░░░██░██████\n" +
            "░██░░░░  ░██   ░██ ░██ ███░██░███████ ░██ ░      ░██░██   ░██░██░░░██\n" +
            "░██      ░██   ░██ ░████░████░██░░░░  ░██    ██  ░██░██   ░██░██  ░██\n" +
            "░██      ░░██████  ███░ ░░░██░░██████░███   ░░█████ ░░██████ ░██████\n" +
            "░░        ░░░░░░  ░░░    ░░░  ░░░░░░ ░░░     ░░░░░   ░░░░░░  ░░░░░\n" +
            "\n" +
            "* Maintainer: tengjiqi@gmail.com & PowerJob-Team\n" +
            "* OfficialWebsite: http://www.powerjob.tech/\n" +
            "* SourceCode: https://github.com/PowerJob/PowerJob\n" +
            "\n";

    public static void print() {
        log.info(BANNER);

        String version = PowerJobWorkerVersion.getVersion();
        version = (version != null) ? " (v" + version + ")" : "";
        log.info(":: PowerJob Worker :: {}", version);
    }

}
