package com.github.kfcfans.oms.server.common.utils;

import com.github.kfcfans.oms.common.ContainerConstant;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * oms-worker container 生成器
 *
 * @author tjq
 * @since 2020/5/15
 */
public class ContainerTemplateGenerator {

    private static final String ORIGIN_FILE_NAME = "oms-template-origin";

    /**
     * 生成 container 的模版文件
     * @param group pom group标签
     * @param artifact pom artifact标签
     * @param name pom name标签
     * @param packageName 包名
     * @param javaVersion Java版本
     * @return 压缩包
     * @throws IOException 异常
     */
    public static File generate(String group, String artifact, String name, String packageName, Integer javaVersion) throws IOException {

        URL resource = ContainerTemplateGenerator.class.getClassLoader().getResource(ORIGIN_FILE_NAME + ".zip");
        if (resource == null) {
            throw new RuntimeException("generate container template failed, can't find zip file in classpath.");
        }
        String originTemplate = resource.getPath();
        ZipFile zipFile = new ZipFile(originTemplate);

        String tmpPath = OmsFileUtils.genTemporaryPath();
        zipFile.extractAll(tmpPath);
        String rootPath = tmpPath + ORIGIN_FILE_NAME;

        // 1. 修改 pom.xml （按行读，读取期间更改，然后回写）
        String pomPath = rootPath + "/pom.xml";

        String line;
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(pomPath))) {
            while ((line = br.readLine()) != null) {

                if (line.contains("<groupId>groupId</groupId>")) {
                    buffer.append("    <groupId>").append(group).append("</groupId>");
                }else if (line.contains("<artifactId>artifactId</artifactId>")) {
                    buffer.append("    <artifactId>").append(artifact).append("</artifactId>");
                }else if (line.contains("<name>name</name>")) {
                    buffer.append("    <name>").append(name).append("</name>");
                }else if (line.contains("<maven.compiler.source>")) {
                    buffer.append("        <maven.compiler.source>").append(javaVersion).append("</maven.compiler.source>");
                }else if (line.contains("<maven.compiler.target>")) {
                    buffer.append("        <maven.compiler.target>").append(javaVersion).append("</maven.compiler.target>");
                } else {
                    buffer.append(line);
                }
                buffer.append(System.lineSeparator());
            }
        }
        OmsFileUtils.string2File(buffer.toString(), new File(pomPath));

        // 2. 新建目录
        String packagePath = StringUtils.replace(packageName, ".", "/");
        String absPath = rootPath + "/src/main/java/" + packagePath;
        FileUtils.forceMkdir(new File(absPath));

        // 3. 修改 Spring 配置文件
        String resourcePath = rootPath + "/src/main/resources/";
        String springXMLPath = resourcePath + ContainerConstant.SPRING_CONTEXT_FILE_NAME;
        buffer.setLength(0);

        try (BufferedReader br = new BufferedReader(new FileReader(springXMLPath))) {
            while ((line = br.readLine()) != null) {

                if (line.contains("<context:component-scan base-package=\"")) {
                    buffer.append("    <context:component-scan base-package=\"").append(packageName).append("\"/>");
                }else {
                    buffer.append(line);
                }
                buffer.append(System.lineSeparator());
            }
        }
        OmsFileUtils.string2File(buffer.toString(), new File(springXMLPath));

        // 4. 写入 packageName，便于容器加载用户类
        String propertiesPath = resourcePath + ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME;
        String properties = ContainerConstant.CONTAINER_PACKAGE_NAME_KEY + "=" + packageName;
        OmsFileUtils.string2File(properties, new File(propertiesPath));

        // 5. 再打包回去
        String finPath = tmpPath + "template.zip";
        ZipFile finZip = new ZipFile(finPath);
        finZip.addFolder(new File(rootPath));

        return finZip.getFile();
    }
}
