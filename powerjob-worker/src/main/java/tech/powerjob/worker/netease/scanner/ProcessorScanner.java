package tech.powerjob.worker.netease.scanner;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;


import java.util.*;

/**
 * @author Echo009
 * @since 2022/1/25
 * <p>
 * 扫描 worker 中存在的 Processor
 */
@Slf4j
@Setter
@NetEaseCustomFeature(CustomFeatureEnum.PROCESSOR_AUTO_REGISTRY)
public class ProcessorScanner {

    private static final String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

    private static final String RESOURCE_PATTERN = "**/*.class";
    /**
     * 官方处理器所在的包
     */
    private static final String OFFICE_PROCESSOR_PACKAGE = "tech.powerjob.official.processors";

    private final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private final CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

    public Set<String> scan(String scanPackages) {
        Set<String> processors = new LinkedHashSet<>();
        for (String scanPackage : getScanPackageList(scanPackages)) {
            try {
                String packageSearchPath = CLASSPATH_ALL_URL_PREFIX +
                        ClassUtils.convertClassNameToResourcePath(scanPackage) + '/' + RESOURCE_PATTERN;
                log.info("[ProcessorScanner] Scan package: {},resource path: {}", scanPackage, packageSearchPath);
                Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
                for (Resource resource : resources) {
                    log.debug("[ProcessorScanner] Scanning {}", resource);
                    if (resource.isReadable()) {
                        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                        ClassMetadata classMetadata = metadataReader.getClassMetadata();
                        if (!classMetadata.isConcrete()) {
                            continue;
                        }
                        String className = classMetadata.getClassName();
                        if (isValidateProcessor(className)) {
                            log.debug("[ProcessorScanner] add {}", className);
                            processors.add(className);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[ProcessorScanner] Fail to scan processor!", e);
                throw new PowerJobException("Fail to scan processor! ");
            }
        }
        return processors;
    }

    private boolean isValidateProcessor(String fullClassName) {
        try {
            Class<?> clz = Class.forName(fullClassName);
            return BasicProcessor.class.isAssignableFrom(clz);
        } catch (Throwable ignore) {
            //
            return false;
        }
    }

    private List<String> getScanPackageList(String scanPackages) {
        if (StringUtils.isEmpty(scanPackages)) {
            return Collections.singletonList(OFFICE_PROCESSOR_PACKAGE);
        }
        String[] splits = scanPackages.split(",");
        ArrayList<String> result = new ArrayList<>(splits.length + 1);
        result.add(OFFICE_PROCESSOR_PACKAGE);
        for (String str : splits) {
            if (StringUtils.isEmpty(str.trim())) {
                continue;
            }
            result.add(str.trim());
        }
        return result;
    }


}
