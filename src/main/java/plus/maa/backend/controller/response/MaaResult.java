package plus.maa.backend.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author AnselYuki
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaaResult<T>(int statusCode, String message, T data) {
    public static <T> MaaResult<T> success(T data) {
        return success(null, data);
    }

    public static <T> MaaResult<T> success(String msg, T data) {
        return new MaaResult<>(200, msg, data);
    }

    public static <T> MaaResult<T> fail(int code, String msg) {
        return fail(code, msg, null);
    }

    public static <T> MaaResult<T> fail(int code, String msg, T data) {
        return new MaaResult<>(code, msg, data);
    }
}