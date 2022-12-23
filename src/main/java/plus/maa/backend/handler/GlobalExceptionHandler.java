package plus.maa.backend.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;

/**
 * @author john180
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ResponseBody
    @ExceptionHandler(value = MaaResultException.class)
    public MaaResult<?> maaExceptionHandler(MaaResultException e) {
        return MaaResult.fail(e.getCode(), e.getMsg(), null);
    }

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public MaaResult<?> defaultExceptionHandler(Exception e, HttpServletRequest request) {
        logError(request);
        log.error("Exception: ", e);
        return MaaResult.fail(500, "服务器内部错误", null);
    }

    private void logError(HttpServletRequest request) {
        log.error("Request URL: {}", request.getRequestURL());
        log.error("Request Method: {}", request.getMethod());
        log.error("Request IP: {}", request.getRemoteAddr());
        log.error("Request Headers: {}", request.getHeaderNames());
        log.error("Request Parameters: {}", request.getParameterMap());
    }
}
