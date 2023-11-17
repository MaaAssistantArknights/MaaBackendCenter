package plus.maa.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.IOException;


/**
 * 提供基于 Etag 机制的 HTTP 缓存，有助于降低网络传输的压力
 *
 * @author lixuhuilll
 */

// 配置需要使用 Etag 机制的 URL，注意和 Spring 的 UrlPattern 语法不太一样
@WebFilter(urlPatterns = {
        "/arknights/level",
        "/copilot/query"
})
@RequiredArgsConstructor
public class MaaEtagHeaderFilter extends ShallowEtagHeaderFilter {

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        // Etag 必须使用弱校验才能与自动压缩兼容
        setWriteWeakETag(true);
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            // ETag 只处理安全的请求
            filterChain.doFilter(request, response);
            return;
        }
        // 允许使用 Etag （实际是避免默认添加的 no-store）
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, max-age=0, must-revalidate");
        // 其他接口默认处理即可，注意默认操作相当于牺牲 CPU 来节约网络带宽，不适用于结果变更过快的接口
        super.doFilterInternal(request, response, filterChain);
    }
}
