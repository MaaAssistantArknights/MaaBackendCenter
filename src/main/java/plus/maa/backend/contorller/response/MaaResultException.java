package plus.maa.backend.contorller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MaaResultException extends RuntimeException {
    private int code;
    private String msg;

    public MaaResultException(String msg) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }
}
