package plus.maa.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import plus.maa.backend.common.MaaStatusCode;

/**
 * @author john180
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MaaResultException extends RuntimeException {
    private final int code;
    private final String msg;

    public MaaResultException(String msg) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    public MaaResultException(MaaStatusCode statusCode) {
        this.code = statusCode.getCode();
        this.msg = statusCode.getMessage();
    }
}
