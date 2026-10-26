package tech.powerjob.worker.sdk;

import com.alibaba.fastjson.JSON;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import tech.powerjob.client.TypeStore;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.HttpUtils;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/17
 */
public abstract class RegistryAppUtils {

    public static void tryRegisterApp(AtomicBoolean flag, String appName, String password, List<String> addressList) {

        HashMap<String, String> request = new HashMap<>();
        request.put("appName", appName);
        request.put("password", password);

        RequestBody requestBody = RequestBody.create(MediaType.parse(OmsConstant.JSON_MEDIA_TYPE), JSON.toJSONString(request));


        for (String address : addressList) {
            try {
                String url = String.format("http://%s%s%s", address, "", "/appInfo/save");
                String post = HttpUtils.post(url, requestBody);
                //JSON
                ResultDTO<Void> result = JSON.parseObject(post, TypeStore.VOID_RESULT_TYPE);
                if (result.isSuccess()) {
                    flag.set(true);
                    break;
                }
            } catch (Exception ignore) {

            }
        }

    }
}
