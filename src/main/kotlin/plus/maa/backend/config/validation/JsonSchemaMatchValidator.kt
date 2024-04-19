package plus.maa.backend.config.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.springframework.core.io.ClassPathResource

class JsonSchemaMatchValidator : ConstraintValidator<JsonSchemaMatch, String> {
    private lateinit var schema: String
    override fun initialize(constraintAnnotation: JsonSchemaMatch) {
        super.initialize(constraintAnnotation)
        schema = constraintAnnotation.schema
    }

    override fun isValid(text: String?, ctx: ConstraintValidatorContext): Boolean {
        try {
            if (text == null) return true
            val validator = validators[schema] ?: return false
            validator.validate(text.let(::JSONObject))
            return true
        } catch (e: Exception) {
            ctx.disableDefaultConstraintViolation()
            ctx.buildConstraintViolationWithTemplate(e.message).addConstraintViolation()
            return false
        }
    }

    companion object {
        const val COPILOT_SCHEMA_JSON = "static/templates/maa-copilot-schema.json"
        val validators = mapOf(
            loadSchema(COPILOT_SCHEMA_JSON),
        )

        @Suppress("SameParameterValue")
        private fun loadSchema(path: String): Pair<String, Schema> {
            val schema = ClassPathResource(path).inputStream.let(::JSONTokener).let(::JSONObject).let(SchemaLoader::load)
            return path to schema
        }
    }
}
