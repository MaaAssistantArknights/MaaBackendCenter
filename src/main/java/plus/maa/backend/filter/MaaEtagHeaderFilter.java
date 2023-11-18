package plus.maa.backend.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        // Etag 必须使用弱校验才能与自动压缩兼容
        setWriteWeakETag(true);
    }

    @Override
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
                                        int responseStatusCode, InputStream inputStream) {

        boolean isMatch = false;

        if (HttpMethod.GET.matches(request.getMethod())) {

            PathContainer pathContainer = PathContainer.parsePath(request.getRequestURI());

            for (PathPattern pattern : CACHE_URI_PATTERNS) {

                if (pattern.matches(pathContainer)) {
                    isMatch = true;
                    break;
                }
            }
        }

        if (isMatch && !response.isCommitted() &&
                responseStatusCode >= 200 && responseStatusCode < 300) {

            String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
            if (cacheControl == null) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_HEAD);
                return true;
            }

            return !cacheControl.contains("no-store");
        }

        return false;
    }
}
