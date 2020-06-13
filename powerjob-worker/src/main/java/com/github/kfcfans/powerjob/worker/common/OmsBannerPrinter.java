package com.github.kfcfans.powerjob.worker.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 打印启动 Banner
 *
 * @author tjq
 * @since 2020/5/11
 */
@Slf4j
public final class OmsBannerPrinter {

    private static final String BANNER = "\n ███████                                          ██          ██     \n" +
            "░██░░░░██                                        ░██         ░██     \n" +
            "░██   ░██  ██████  ███     ██  █████  ██████     ░██  ██████ ░██     \n" +
            "░███████  ██░░░░██░░██  █ ░██ ██░░░██░░██░░█     ░██ ██░░░░██░██████ \n" +
            "░██░░░░  ░██   ░██ ░██ ███░██░███████ ░██ ░      ░██░██   ░██░██░░░██\n" +
            "░██      ░██   ░██ ░████░████░██░░░░  ░██    ██  ░██░██   ░██░██  ░██\n" +
            "░██      ░░██████  ███░ ░░░██░░██████░███   ░░█████ ░░██████ ░██████ \n" +
            "░░        ░░░░░░  ░░░    ░░░  ░░░░░░ ░░░     ░░░░░   ░░░░░░  ░░░░░   \n";

    public static void print() {
        log.info(BANNER);

        String version = OmsWorkerVersion.getVersion();
        version = (version != null) ? " (v" + version + ")" : "";
        log.info(":: OhMyScheduler Worker :: {}", version);
    }

}
