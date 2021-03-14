package tech.powerjob.worker.common;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * 获取 Worker 版本，便于开发者排查问题
 *
 * @author tjq
 * @since 2020/5/11
 */
public final class PowerJobWorkerVersion {

    private static String CACHE = null;

    /**
     * Return the full version string of the present OhMyScheduler-Worker codebase, or {@code null}
     * if it cannot be determined.
     * @return the version of OhMyScheduler-Worker or {@code null}
     * @see Package#getImplementationVersion()
     */
    public static String getVersion() {
        if (StringUtils.isEmpty(CACHE)) {
            CACHE = determinePowerJobVersion();
        }
        return CACHE;
    }

    private static String determinePowerJobVersion() {
        String implementationVersion = PowerJobWorkerVersion.class.getPackage().getImplementationVersion();
        if (implementationVersion != null) {
            return implementationVersion;
        }
        CodeSource codeSource = PowerJobWorkerVersion.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL codeSourceLocation = codeSource.getLocation();
        try {
            URLConnection connection = codeSourceLocation.openConnection();
            if (connection instanceof JarURLConnection) {
                return getImplementationVersion(((JarURLConnection) connection).getJarFile());
            }
            try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
                return getImplementationVersion(jarFile);
            }
        }
        catch (Exception ex) {
            return null;
        }
    }

    private static String getImplementationVersion(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }
}
