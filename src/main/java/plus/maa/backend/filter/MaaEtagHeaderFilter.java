package plus.maa.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


/**
 * 提供基于 Etag 机制的 HTTP 缓存，有助于降低网络传输的压力
 *
 * @author lixuhuilll
 */

@Component
public class MaaEtagHeaderFilter extends ShallowEtagHeaderFilter {

    private static final String CACHE_HEAD = "private, no-cache, max-age=0, must-revalidate";

    // 配置需要使用 Etag 机制的 URI，注意和 Spring 的 UrlPattern 语法不太一样
    private static final Set<String> CACHE_URI = Set.of(
            "/arknights/level",
            "/copilot/query"
    );

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        // Etag 必须使用弱校验才能与自动压缩兼容
        setWriteWeakETag(true);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (CACHE_URI.contains(request.getRequestURI())) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_HEAD);
        }
        // 其他接口默认处理即可，注意默认操作相当于牺牲 CPU 来节约网络带宽，不适用于结果变更过快的接口
        super.doFilterInternal(request, response, filterChain);
    }

    @Override
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
                                        int responseStatusCode, InputStream inputStream) {
        return CACHE_URI.contains(request.getRequestURI()) &&
                super.isEligibleForEtag(request, response, responseStatusCode, inputStream);
    }
}
