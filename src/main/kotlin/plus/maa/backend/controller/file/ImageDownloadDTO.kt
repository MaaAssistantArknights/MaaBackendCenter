package plus.maa.backend.controller.file

import jakarta.validation.constraints.NotNull

/**
 * @author LoMu
 * Date  2023-04-16 17:41
 */
data class ImageDownloadDTO(
    @field:NotNull
    val type: String,
    val classification: String? = null,
    val version: List<String>? = null,
    val label: String? = null,
    val delete: Boolean = false
)
