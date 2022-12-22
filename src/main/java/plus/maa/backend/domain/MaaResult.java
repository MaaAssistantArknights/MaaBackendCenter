package plus.maa.backend.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author AnselYuki
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaaResult {
    @JsonAlias("status_code")
    private Integer statusCode;
    private String message;
    private Object data;

    public MaaResult(int statusCode, String message, Object data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    public MaaResult(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}