package plus.maa.backend.service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import plus.maa.backend.controller.response.MaaResult;

/**
 * @author FAll
 * @date 2022/12/23 10:10
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * @author FAll
     * @description 请求参数缺失
     * @param e MissingServletRequestParameterException
     * @param request HttpServletRequest
     * @return: plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @date 2022/12/23 9:50
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public MaaResult<String> missingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("请求参数缺失", e);
        return MaaResult.fail(405,String.format("请求参数缺失:%s", e.getParameterName()));
    }

    /**
     * @author FAll
     * @description 参数类型不匹配
     * @return: plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @date 2022/12/23 9:55
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public MaaResult<String> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配", e);
        return MaaResult.fail(405,String.format("参数类型不匹配:%s", e.getMessage()));
    }

    /**
     * @author FAll
     * @description 参数校验错误
     * @return: plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @date 2022/12/23 10:01
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MaaResult<String> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验错误", e);
        FieldError fieldError = e.getBindingResult().getFieldError();
        assert fieldError != null;
        return MaaResult.fail(405,String.format("参数校验错误:%s", fieldError.getDefaultMessage()));
    }

    /**
     * @author FAll
     * @description
     * @return: plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @date 2022/12/23 10:12
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public MaaResult<String> noHandlerFoundExceptionHandler(NoHandlerFoundException e) {
        log.warn("请求地址不存在", e);
        return MaaResult.fail(404,String.format("请求地址 %s 不存在", e.getRequestURL()));
    }

    /**
     * @author FAll
     * @description
     * @return: plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @date 2022/12/23 10:13
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public MaaResult<String> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方式错误", e);
        return MaaResult.fail(405,String.format("请求方法不正确:%s", e.getMessage()));
    }

    /**
     * @author FAll
     * @description
     * @return: plus.maa.backend.controller.response.MaaResult<java.lang.String>
     * @date 2022/12/23 10:15
     */
    @ExceptionHandler(Exception.class)
    public MaaResult<String> exceptionHandler(Exception e) {
        log.error(" 内部错误: {}", e.getMessage(), e);
        return MaaResult.fail(500,"内部错误");
    }

}
