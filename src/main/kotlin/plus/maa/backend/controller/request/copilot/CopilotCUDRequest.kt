package plus.maa.backend.controller.request.copilot

import plus.maa.backend.config.validation.JsonSchemaMatch
import plus.maa.backend.config.validation.JsonSchemaMatchValidator

data class CopilotCUDRequest(
    @JsonSchemaMatch(schema = JsonSchemaMatchValidator.COPILOT_SCHEMA_JSON)
    val content: String? = null,
    val id: Long? = null,
)
