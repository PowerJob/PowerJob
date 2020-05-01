package com.github.kfcfans.oms.common.response;

import com.github.kfcfans.oms.common.OmsSerializable;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import lombok.*;


/**
 * Pattens.ask 的响应
 *
 * @author tjq
 * @since 2020/3/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse implements OmsSerializable {

    private boolean success;

    /*
    - 使用 Object 会报错：java.lang.ClassCastException: scala.collection.immutable.HashMap cannot be cast to XXX，只能自己序列化反序列化了
    - 嵌套类型（比如 Map<String, B>），如果B也是个复杂对象，那么反序列化后B的类型为 LinkedHashMap... 处理比较麻烦（转成JSON再转回来）
     */
    private byte[] data;

    // 错误信息
    private String message;

    public static AskResponse succeed(Object data) {
        AskResponse r = new AskResponse();
        r.success = true;
        if (data != null) {
            r.data = JsonUtils.toBytes(data);
        }
        return r;
    }

    public static AskResponse failed(String msg) {
        AskResponse r = new AskResponse();
        r.success = false;
        r.message = msg;
        return r;
    }

    public <T> T getData(Class<T> clz) throws Exception {
        return JsonUtils.parseObject(data, clz);
    }

}
