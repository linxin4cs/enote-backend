package sit.zlx.enotebackend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sit.zlx.enotebackend.dto.ResponseDTO;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<?>> handleException(Exception e) {
        // 记录异常信息到日志中
//        Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
//        logger.error("Unhandled exception occurred: ", ex);

        // 返回一个通用的错误响应，不包含技术细节
        ResponseDTO<?> response = new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(),
                new ResponseDTO.ResponseData<>("服务器错误，请联系管理员", null));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<?>> handleMissingRequestBody(HttpMessageNotReadableException ex) {
        ResponseDTO<?> response = new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(),
                new ResponseDTO.ResponseData<>("请求体缺失或格式不正确", null));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
