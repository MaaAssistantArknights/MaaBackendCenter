package plus.maa.backend.controller.file

import jakarta.validation.constraints.NotNull

class UploadAbility(
    /**
     * 是否开启上传功能
     */
    @field:NotNull
    var enabled: Boolean
)
