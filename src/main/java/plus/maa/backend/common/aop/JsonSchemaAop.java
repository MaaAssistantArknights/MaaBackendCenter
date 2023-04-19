package plus.maa.backend.common.aop;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.annotation.JsonSchema;
import plus.maa.backend.controller.request.comments.CommentsRatingDTO;
import plus.maa.backend.controller.request.copilot.CopilotCUDRequest;
import plus.maa.backend.controller.request.copilot.CopilotRatingReq;
import plus.maa.backend.controller.response.MaaResultException;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author LoMu
 * Date  2023-01-22 17:53
 */

@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class JsonSchemaAop {
    private final ObjectMapper mapper;
    private static final String COPILOT_SCHEMA_JSON = "static/templates/maa-copilot-schema.json";
    private static final String RATING_SCHEMA_JSON = "static/templates/maa-rating-schema.json";

    @Pointcut("@annotation(plus.maa.backend.common.annotation.JsonSchema)")
    public void pt() {
    }

    /**
     * 数据校验
     *
     * @param joinPoint  形参
     * @param jsonSchema 注解
     */
    @Before("pt() && @annotation(jsonSchema)")
    public void before(JoinPoint joinPoint, JsonSchema jsonSchema) {
        String schema_json = null;
        String content = null;
        //获取 CopilotCUDRequest形参的index
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof CopilotCUDRequest) {
                content = ((CopilotCUDRequest) arg).getContent();
                schema_json = COPILOT_SCHEMA_JSON;
            }
            if (arg instanceof CopilotRatingReq || arg instanceof CommentsRatingDTO) {
                try {
                    schema_json = RATING_SCHEMA_JSON;
                    content = mapper.writeValueAsString(arg);
                } catch (JsonProcessingException e) {
                    log.error("json解析失败", e);
                }
            }
        }
        if (content == null) return;


        //获取json schema json路径并验证
        try (InputStream inputStream = new ClassPathResource(schema_json).getInputStream()) {
            JSONObject json = new JSONObject(content);
            JSONObject jsonObject = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(jsonObject);
            schema.validate(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ValidationException e) {
            throw new MaaResultException(HttpStatus.BAD_REQUEST.value(), "数据不符合规范，请前往前端作业编辑器进行操作");
        }
    }
}