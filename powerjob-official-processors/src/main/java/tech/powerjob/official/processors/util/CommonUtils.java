package tech.powerjob.official.processors.util;

import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import org.apache.commons.lang3.StringUtils;

/**
 * CommonUtils
 *
 * @author tjq
 * @since 2021/2/1
 */
public class CommonUtils {

    public static String parseParams(TaskContext context) {
        if (StringUtils.isEmpty(context.getInstanceParams())) {
            return context.getInstanceParams();
        }
        return context.getJobParams();
    }
}
