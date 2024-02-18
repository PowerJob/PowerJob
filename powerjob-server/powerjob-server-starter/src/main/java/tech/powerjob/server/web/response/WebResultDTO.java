package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;
import tech.powerjob.common.response.ResultDTO;

/**
 * WEB 请求结果
 *
 * @author tjq
 * @since 2024/2/18
 */
@Getter
@Setter
public class WebResultDTO<T> extends ResultDTO<T> {

    private String code;

    public WebResultDTO() {
    }

    public WebResultDTO(ResultDTO<T> res) {
        this.success = res.isSuccess();
        this.data = res.getData();
        this.message  = res.getMessage();
    }
}
