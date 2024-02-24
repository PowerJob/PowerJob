package tech.powerjob.server.remote.worker.utils;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.common.module.WorkerInfo;

import java.util.Optional;
import java.util.Set;

/**
 * 指定工具
 *
 * @author tjq
 * @since 2024/2/24
 */
public class SpecifyUtils {

    private static final String TAG_EQUALS = "tagEquals:";

    private static final String TAG_IN = "tagIn:";

    public static boolean match(WorkerInfo workerInfo, String specifyInfo) {

        String workerTag = workerInfo.getTag();

        // tagIn 语法，worker 可上报多个tag，如 WorkerInfo#tag=tag1,tag2,tag3，配置中指定 tagIn=tag1 即可命中
        if (specifyInfo.startsWith(TAG_IN)) {
            String targetTag = specifyInfo.replace(TAG_IN, StringUtils.EMPTY);
            return Optional.ofNullable(workerTag).orElse(StringUtils.EMPTY).contains(targetTag);
        }

        // tagEquals 语法，字符串完全匹配，worker 只可上报一个 tag，如 WorkerInfo#tag=tag1，配置中指定 tagEquals=tag1 即可命中
        if (specifyInfo.startsWith(TAG_EQUALS)) {
            String targetTag = specifyInfo.replace(TAG_EQUALS, StringUtils.EMPTY);
            return Optional.ofNullable(workerTag).orElse(StringUtils.EMPTY).equals(targetTag);
        }

        // 默认情况，IP 和 tag 逗号分割后任意完全匹配即视为命中（兼容 4.3.8 版本前序逻辑）
        Set<String> designatedWorkersSet = Sets.newHashSet(SJ.COMMA_SPLITTER.splitToList(specifyInfo));

        for (String tagOrAddress : designatedWorkersSet) {
            if (tagOrAddress.equals(workerInfo.getTag()) || tagOrAddress.equals(workerInfo.getAddress())) {
                return true;
            }
        }

        return false;
    }

}
