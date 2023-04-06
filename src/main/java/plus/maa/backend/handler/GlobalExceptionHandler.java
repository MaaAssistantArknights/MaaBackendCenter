package plus.maa.backend.handler;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;

/**
 * @author john180
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @return plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @author FAll
     * @description 请求参数缺失
     * @date 2022/12/23 12:00
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public MaaResult<String> missingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                     HttpServletRequest request) {
        logWarn(request);
        log.warn("请求参数缺失", e);
        return MaaResult.fail(400, String.format("请求参数缺失:%s", e.getParameterName()));
    }

    /**
     * @return plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @author FAll
     * @description 参数类型不匹配
     * @date 2022/12/23 12:01
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public MaaResult<String> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                                 HttpServletRequest request) {
        logWarn(request);
        log.warn("参数类型不匹配", e);
        return MaaResult.fail(400, String.format("参数类型不匹配:%s", e.getMessage()));
    }

    /**
     * @return plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @author FAll
     * @description 参数校验错误
     * @date 2022/12/23 12:02
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MaaResult<String> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            return MaaResult.fail(400, String.format("参数校验错误:%s", fieldError.getDefaultMessage()));
        }
        return MaaResult.fail(400, String.format("参数校验错误:%s", e.getMessage()));
    }

    /**
     * @return plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @author FAll
     * @description 请求地址不存在
     * @date 2022/12/23 12:03
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public MaaResult<String> noHandlerFoundExceptionHandler(NoHandlerFoundException e) {
        log.warn("请求地址不存在", e);
        return MaaResult.fail(404, String.format("请求地址 %s 不存在", e.getRequestURL()));
    }

    /**
     * @return plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @author FAll
     * @description
     * @date 2022/12/23 12:04
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public MaaResult<String> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e,
                                                                           HttpServletRequest request) {
        logWarn(request);
        log.warn("请求方式错误", e);
        return MaaResult.fail(405, String.format("请求方法不正确:%s", e.getMessage()));
    }

    /**
     * 处理由 {@link org.springframework.util.Assert} 工具产生的异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public MaaResult<String> illegalArgumentOrStateExceptionHandler(RuntimeException e) {
        return MaaResult.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /**
     * @return plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @author cbc
     * @description
     * @date 2022/12/26 12:00
     */
    @ExceptionHandler(MaaResultException.class)
    public MaaResult<String> maaResultExceptionHandler(MaaResultException e) {
        return MaaResult.fail(e.getCode(), e.getMsg());
    }

    /**
     * @author john180
     * @description 用户鉴权相关，异常兜底处理
     */
    @ExceptionHandler(AuthenticationException.class)
    public MaaResult<String> authExceptionHandler(AuthenticationException e) {
        return MaaResult.fail(401, e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public MaaResult<String> fileSizeThresholdHandler(MultipartException e) {
        return MaaResult.fail(413, e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public MaaResult<String> handleResponseStatusException(ResponseStatusException ex) {
        return MaaResult.fail(ex.getStatusCode().value(), ex.getMessage());
    }

    /**
     * @return plus.maa.backend.controller.response.MaaResult<?>
     * @author john180
     * @description 服务器内部错误，异常兜底处理
     * @date 2022/12/23 12:06
     */
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public MaaResult<?> defaultExceptionHandler(Exception e,
                                                HttpServletRequest request) {
        logError(request);
        log.error("Exception: ", e);
        return MaaResult.fail(500, "服务器内部错误", null);
    }

    private void logWarn(HttpServletRequest request) {
        log.warn("Request URL: {}", request.getRequestURL());
        log.warn("Request Method: {}", request.getMethod());
        log.warn("Request IP: {}", request.getRemoteAddr());
        log.warn("Request Headers: {}", request.getHeaderNames());
        log.warn("Request Parameters: {}", request.getParameterMap());
    }

    private void logError(HttpServletRequest request) {
        log.error("Request URL: {}", request.getRequestURL());
        log.error("Request Method: {}", request.getMethod());
        log.error("Request IP: {}", request.getRemoteAddr());
        log.error("Request Headers: {}", request.getHeaderNames());
        log.error("Request Parameters: {}", request.getParameterMap());
    }
}
