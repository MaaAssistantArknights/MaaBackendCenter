package plus.maa.backend.common.aop

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import plus.maa.backend.common.annotation.JsonSchema
import plus.maa.backend.controller.request.comments.CommentsRatingDTO
import plus.maa.backend.controller.request.copilot.CopilotCUDRequest
import plus.maa.backend.controller.request.copilot.CopilotRatingReq
import plus.maa.backend.controller.response.MaaResultException
import java.io.IOException

private val log = KotlinLogging.logger { }

/**
 * @author LoMu
 * Date  2023-01-22 17:53
 */
@Component
@Aspect
class JsonSchemaAop(
    private val mapper: ObjectMapper
) {
    @Pointcut("@annotation(plus.maa.backend.common.annotation.JsonSchema)")
    fun pt() {
    }

    /**
     * 数据校验
     *
     * @param joinPoint  形参
     * @param jsonSchema 注解
     */
    @Before("pt() && @annotation(jsonSchema)")
    fun before(joinPoint: JoinPoint, jsonSchema: JsonSchema?) {
        var schemaJson: String? = null
        var content: String? = null
        //判断是验证的是Copilot还是Rating
        for (arg in joinPoint.args) {
            if (arg is CopilotCUDRequest) {
                content = arg.content
                schemaJson = COPILOT_SCHEMA_JSON
            }
            if (arg is CopilotRatingReq || arg is CommentsRatingDTO) {
                try {
                    schemaJson = RATING_SCHEMA_JSON
                    content = mapper.writeValueAsString(arg)
                } catch (e: JsonProcessingException) {
                    log.error(e) { "json解析失败" }
                }
            }
        }
        if (schemaJson == null || content == null) return


        //获取json schema json路径并验证
        try {
            ClassPathResource(schemaJson).inputStream.use { inputStream ->
                val json = JSONObject(content)
                val jsonObject = JSONObject(JSONTokener(inputStream))
                val schema = SchemaLoader.load(jsonObject)
                schema.validate(json)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: ValidationException) {
            log.warn { "schema Location: ${e.violatedSchema.schemaLocation}" }
            throw MaaResultException(HttpStatus.BAD_REQUEST.value(), "数据不符合规范，请前往前端作业编辑器进行操作")
        }
    }

    companion object {
        private const val COPILOT_SCHEMA_JSON = "static/templates/maa-copilot-schema.json"
        private const val RATING_SCHEMA_JSON = "static/templates/maa-rating-schema.json"
    }
}