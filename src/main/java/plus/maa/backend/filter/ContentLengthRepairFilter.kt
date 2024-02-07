package plus.maa.backend.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.ShallowEtagHeaderFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.IOException
import java.io.InputStream

/**
 * 解决了 GZIP 无法对 JSON 响应正常处理 min-response-size 的问题，
 * 借助了 ETag 处理流程中的 Response 包装类包装所有响应，
 * 从而正常获取 Content-Length
 *
 * @author lixuhuilll
 */
@Component
@ConditionalOnProperty(name = ["server.compression.enabled"], havingValue = "true")
class ContentLengthRepairFilter : ShallowEtagHeaderFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (response is ContentCachingResponseWrapper) {
            // 不对已包装过的响应体做处理
            filterChain.doFilter(request, response)
        } else {
            super.doFilterInternal(request, response, filterChain)
        }
    }

    override fun isEligibleForEtag(
        request: HttpServletRequest,
        response: HttpServletResponse,
        responseStatusCode: Int,
        inputStream: InputStream
    ) = false
}
