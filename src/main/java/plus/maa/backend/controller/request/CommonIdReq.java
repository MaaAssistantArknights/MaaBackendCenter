package plus.maa.backend.controller.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author dragove
 * create on 2024-01-05
 */
@Getter
@Setter
public class CommonIdReq<T> {

    @NotNull(message = "id必填")
    private T id;

}
