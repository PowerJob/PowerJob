package tech.powerjob.worker.container;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类加载器
 * 未破坏双亲委派模型，可能带来相同类ClassNotFoundException的后果（比如不同版本互不兼容的工具类）
 * 为什么不破坏？
 *     1. 破坏以后，容器需要加载更多的类，meta space说不定就要爆了...
 *     2. 公共类与Worker保持一致即可解决问题，且目测CNF概率不会很高。
 *
 * @author tjq
 * @since 2020/3/23
 */
@Slf4j
public class OhMyClassLoader extends URLClassLoader {

    public OhMyClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * 主动加载类，否则类加载器内空空如也，Spring IOC容器初始化不到任何东西
     * @param packageName 包路径，主动加载用户写的类
     * @throws Exception 加载异常
     */
    public void load(String packageName) throws Exception {
        URL[] urLs = getURLs();
        for (URL jarURL : urLs) {
            JarFile jarFile = new JarFile(new File(jarURL.toURI()));
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String name = jarEntry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }

                // 转换 org/spring/AAA.class -> org.spring.AAA
                String tmp = name.substring(0, name.length() - 6);
                String res = StringUtils.replace(tmp, "/", ".");

                if (res.startsWith(packageName)) {
                    loadClass(res);
                    log.info("[OhMyClassLoader] load class({}) successfully.", res);
                }
            }
        }
    }
}
