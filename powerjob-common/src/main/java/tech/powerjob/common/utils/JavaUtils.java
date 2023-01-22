package tech.powerjob.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Java 语言相关的工具
 *
 * @author tjq
 * @since 2022/10/23
 */
@Slf4j
public class JavaUtils {

    /**
     * 获取类所在 Jar 包的版本
     * @param clz 类
     * @return 包版本
     */
    public static String determinePackageVersion(Class<?> clz) {
        try {

            String implementationVersion = clz.getPackage().getImplementationVersion();
            if (implementationVersion != null) {
                return implementationVersion;
            }
            CodeSource codeSource = clz.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URL codeSourceLocation = codeSource.getLocation();

            URLConnection connection = codeSourceLocation.openConnection();
            if (connection instanceof JarURLConnection) {
                return getImplementationVersion(((JarURLConnection) connection).getJarFile());
            }
            final File file = new File(codeSourceLocation.toURI());
            // idea 场景，查找版本失败
            if (!file.exists() || file.isDirectory()) {
                return "UNKNOWN";
            }
            try (JarFile jarFile = new JarFile(file)) {
                return getImplementationVersion(jarFile);
            }
        }
        catch (Throwable t) {
            log.warn("[JavaUtils] determinePackageVersion for clz[{}] failed, msg: {}", clz.getSimpleName(), t.toString());
            // windows 下无权限访问会一直报错一直重试，需要在此兼容
            return "UNKNOWN";
        }
    }
    private static String getImplementationVersion(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }
}
