package sit.zlx.enotebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
public class ResponseDTO<T> {
    private int code;
    private ResponseData<T> data;

    @Getter
    @AllArgsConstructor
    public enum STATUS_CODE {
        SUCCESS(200, "操作成功"),
        BAD_REQUEST(400, "错误的请求"),
        UNAUTHORIZED(401, "未授权"),
        FORBIDDEN(403, "禁止访问"),
        NOT_FOUND(404, "资源未找到"),
        INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
        CUSTOM_BUSINESS_ERROR(2001, "自定义业务错误");

        private final int code;
        private final String message;
    }

    @Getter
    @AllArgsConstructor
    public static class ResponseData<T> {
        private String message;
        private T data;
    }
}

