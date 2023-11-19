package plus.maa.backend.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.InputStream;
import java.util.List;
import java.util.Set;


/**
 * 提供基于 Etag 机制的 HTTP 缓存，有助于降低网络传输的压力
 * <p>
 * 同时还解决了 GZIP 无法对 JSON 响应正常处理 min-response-size 的问题，
 * 借助了 ETag 处理流程中的 Response 包装类包装所有响应，
 * 从而正常获取 Content-Length
 *
 * @author lixuhuilll
 */

@Component
public class MaaEtagHeaderFilter extends ShallowEtagHeaderFilter {

    /**
     * 配置需要使用 Etag 机制的 URI，采用 PathPatter 语法
     *
     * @see PathPattern
     */
    private static final Set<String> CACHE_URI = Set.of(
            "/arknights/level",
            "/copilot/query"
    );

    private static final String CACHE_HEAD = "private, no-cache, max-age=0, must-revalidate";

    private static final List<PathPattern> CACHE_URI_PATTERNS = CACHE_URI.stream()
            .map(PathPatternParser.defaultInstance::parse)
            .toList();

    @Override
    protected void initFilterBean() {
        // Etag 必须使用弱校验才能与自动压缩兼容
        setWriteWeakETag(true);
    }

    @Override
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
                                        int responseStatusCode, InputStream inputStream) {

        if (super.isEligibleForEtag(request, response, responseStatusCode, inputStream)) {

            // 如果该请求符合产生 ETag 的条件，判断是否为需要使用 ETag 机制的 URI
            PathContainer pathContainer = PathContainer.parsePath(request.getRequestURI());
            for (PathPattern pattern : CACHE_URI_PATTERNS) {

                if (pattern.matches(pathContainer)) {
                    // 如果是需要使用 ETag 机制的 URI，若其响应中不存在缓存控制头，则配置默认值
                    String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
                    if (cacheControl == null) {
                        response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_HEAD);
                    }
                    // 不论是否进行默认值处理，均返回 true
                    return true;
                }
            }
        }

        return false;
    }
}
