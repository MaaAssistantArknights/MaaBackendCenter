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
import org.springframework.stereotype.Component;
import plus.maa.backend.common.annotation.JsonSchema;
import plus.maa.backend.controller.request.CopilotCUDRequest;
import plus.maa.backend.controller.response.MaaResultException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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
        final String COPILOT_SCHEMA_JSON = "static/templates/maa-copilot-schema.json";

        //获取 CopilotCUDRequest形参的index
        int index = jsonSchema.index();
        CopilotCUDRequest request = (CopilotCUDRequest) joinPoint.getArgs()[index];
        String json = null;
        if (!Objects.isNull(request)) {
            try {
                json = mapper.writeValueAsString(request.getContent());
            } catch (JsonProcessingException e) {
                log.error("json序列化失败", e);
            }
            //获取json schema json路径并验证
            try (InputStream inputStream = new ClassPathResource(COPILOT_SCHEMA_JSON).getInputStream()) {
                JSONObject jsonObject = new JSONObject(new JSONTokener(inputStream));
                Schema schema = SchemaLoader.load(jsonObject);
                schema.validate(json);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ValidationException e) {
                throw new MaaResultException("数据不符合规范，请前往前端作业编辑器进行操作");
            }

        }
    }
}