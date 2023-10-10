package plus.maa.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import plus.maa.backend.repository.RedisCache;

import java.io.IOException;


/**
 * 提供基于 Etag 机制的 HTTP 缓存，有助于降低网络传输的压力
 *
 * @author lixuhuilll
 */

// 配置需要使用 Etag 机制的 URL，注意和 Spring 的 UrlPattern 语法不太一样
@WebFilter(urlPatterns = {
        "/arknights/level",
        "/copilot/query",
        "/copilot/get/*"
})
@RequiredArgsConstructor
public class MaaEtagHeaderFilter extends ShallowEtagHeaderFilter {

    private final RedisCache redisCache;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        // 允许使用 Etag （实际是避免默认添加的 no-store）
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate");
        // 为 ArkLevel 进行特殊处理，因为它在 Redis 中存在指示变更的值，可以借此节约性能
        if ("/arknights/level".equals(request.getRequestURI())) {
            String levelCommit = redisCache.getCacheLevelCommit();
            levelCommit = levelCommit == null ? "0" : levelCommit;
            WebRequest webRequest = new ServletWebRequest(request, response);
            if (webRequest.checkNotModified(levelCommit)) {
                // 只要 Redis 中指示的 levelCommit 未变更，则认为可以使用浏览器缓存的地图数据
                return;
            }
            // levelCommit 发生变更或当前用户不存在缓存，响应新数据
            filterChain.doFilter(request, response);
            return;
        }
        // 其他接口默认处理即可，注意默认操作相当于牺牲 CPU 来节约网络带宽，不适用于结果变更过快的接口
        super.doFilterInternal(request, response, filterChain);
    }
}
