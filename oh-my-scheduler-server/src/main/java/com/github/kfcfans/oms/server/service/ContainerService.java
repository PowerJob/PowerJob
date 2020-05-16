package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.oms.common.utils.CommonUtils;
import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 容器服务
 *
 * @author tjq
 * @since 2020/5/16
 */
@Slf4j
@Service
public class ContainerService {

    private GridFsTemplate gridFsTemplate;

    /**
     * 获取构建容器所需要的 Jar 文件
     * @param md5 Jar文件的MD5值，可以由此构建 mongoDB 文件名
     * @return 本地Jar文件
     */
    public File fetchContainerJarFile(String md5) {

        String jarFileName = OmsFileUtils.genContainerJarPath() + genContainerJarName(md5);
        File jarFile = new File(jarFileName);

        if (jarFile.exists()) {
            return jarFile;
        }
        if (gridFsTemplate != null) {
            downloadJarFromMongoDB(genContainerJarName(md5), jarFile);
        }
        return jarFile;
    }

    private void downloadJarFromMongoDB(String mongoFileName, File targetFile) {
        synchronized (mongoFileName.intern()) {
            if (targetFile.exists()) {
                return;
            }
            GridFsResource gridFsResource = gridFsTemplate.getResource(mongoFileName);
            if (!gridFsResource.exists()) {
                log.warn("[ContainerService] can't find container's jar file({}) in gridFS.", mongoFileName);
                return;
            }
            try {
                OmsFileUtils.gridFs2File(gridFsResource, targetFile);
            }catch (Exception e) {
                CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(targetFile));
                throw e;
            }
        }
    }


    private static String genContainerJarName(String md5) {
        return String.format("oms-container-%s.jar", md5);
    }

    @Autowired(required = false)
    public void setGridFsTemplate(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }
}
