package tech.powerjob.worker.autoconfigure.registry;

import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.HttpUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/17
 */
@Slf4j
public abstract class RegistryAppUtils {

    public static void tryRegisterApp(String appName, String password, List<String> addressList) {


        List<Exception> exceptions = new ArrayList<>(addressList.size());

        for (String address : addressList) {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("appName", appName)
                        .add("password", password)
                        .build();

                String url = String.format("http://%s%s%s", address, "", "/appInfo/save");
                HttpUtils.post(url, body);
            } catch (Exception err) {
                exceptions.add(err);
            }
        }

        if (exceptions.isEmpty()) {
            log.info("[PowerJobRegistry] 注册App成功,appName:({}),password:({}),serverAddress:({})", appName, password, addressList);
        } else {
            PowerJobException powerJobException = new PowerJobException("registry failed.");
            exceptions.forEach(powerJobException::addSuppressed);
            log.error("[PowerJobRegistry] 注册App失败,appName:({}),password:({}),serverAddress:({})", appName, password, addressList, powerJobException);
        }
    }
}
