package plus.maa.backend.config.doc;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.*;

/**
 * 指示需要 Jwt 认证
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_JWT)
public @interface RequireJwt {
}
