package tech.powerjob.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Java 语言相关的工具测试
 *
 * @author tjq
 * @since 2022/10/23
 */
@Slf4j
class JavaUtilsTest {

    @Test
    void determinePackageVersion() {

        String packageVersion = JavaUtils.determinePackageVersion(LoggerFactory.class);
        log.info("[determinePackageVersion] LoggerFactory's package version: {}", packageVersion);
    }
}