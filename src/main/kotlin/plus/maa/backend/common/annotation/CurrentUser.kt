package plus.maa.backend.common.annotation

import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * @author john180
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@AuthenticationPrincipal
annotation class CurrentUser
