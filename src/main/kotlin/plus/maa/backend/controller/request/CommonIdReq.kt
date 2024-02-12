package plus.maa.backend.controller.request

import jakarta.validation.constraints.NotNull

/**
 * @author dragove
 * create on 2024-01-05
 */
data class CommonIdReq<T>(
    @field:NotNull(message = "id必填")
    val id: T
)
