package com.github.kfcfans.powerjob.server;

import com.github.kfcfans.powerjob.server.akka.OhMyServer;
import com.github.kfcfans.powerjob.server.common.utils.OmsFileUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

/**
 * SpringBoot 启动入口
 *
 * @author tjq
 * @since 2020/3/29
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class OhMyApplication {

    private static final String TIPS = "\n\n" +
            "******************* PowerJob Tips *******************\n" +
            "如果应用无法启动，我们建议您仔细阅读以下文档来解决:\n" +
            "if server can't startup, we recommend that you read the documentation to find a solution:\n" +
            "https://www.yuque.com/powerjob/guidence/xp5ygc#xMQC9\n" +
            "******************* PowerJob Tips *******************\n\n";

    public static void main(String[] args) {

        // 完成前置工作
        pre();

        // 先启动 ActorSystem
        OhMyServer.init();

        // 再启动SpringBoot
        try {
            SpringApplication.run(OhMyApplication.class, args);
        }catch (Throwable t) {
            log.error(TIPS);
            throw t;
        }
    }

    @VisibleForTesting
    public static void pre() {
        log.info(TIPS);

        // 删除历史遗留的 H2 数据库文件
        File oldH2 = new File(OmsFileUtils.genH2BasePath());
        try {
            if (oldH2.exists()) {
                FileUtils.forceDelete(oldH2);
            }
        }catch (Exception e) {
            log.warn("[PowerJob] delete h2 workspace({}) failed, if server can't startup successfully, please delete it manually", oldH2, e);
        }
    }

}
