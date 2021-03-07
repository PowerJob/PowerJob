package tech.powerjob.server.remote.server.redirector;

import com.alibaba.fastjson.JSONObject;
import tech.powerjob.server.common.utils.SpringUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * process remote request
 *
 * @author tjq
 * @since 2021/2/19
 */
public class RemoteRequestProcessor {

    public static Object processRemoteRequest(RemoteProcessReq req) throws ClassNotFoundException {
        Object[] args = req.getArgs();
        String[] parameterTypes = req.getParameterTypes();
        Class<?>[] parameters = new Class[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = Class.forName(parameterTypes[i]);
            Object arg = args[i];
            if (arg != null) {
                args[i] = JSONObject.parseObject(JSONObject.toJSONBytes(arg), parameters[i]);
            }
        }

        Class<?> clz = Class.forName(req.getClassName());

        Object bean = SpringUtils.getBean(clz);
        Method method = ReflectionUtils.findMethod(clz, req.getMethodName(), parameters);

        assert method != null;
        return ReflectionUtils.invokeMethod(method, bean, args);
    }
}
