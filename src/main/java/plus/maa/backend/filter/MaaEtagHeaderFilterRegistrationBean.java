package plus.maa.backend.filter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.InputStream;
import java.util.Set;


/**
 * 提供基于 Etag 机制的 HTTP 缓存，有助于降低网络传输的压力
 *
 * @author lixuhuilll
 */

@Component
public class MaaEtagHeaderFilterRegistrationBean extends FilterRegistrationBean<Filter> {

    /**
     * 配置需要使用 Etag 机制的 URI，采用 Servlet 的 URI 匹配语法
     */
    private final Set<String> ETAG_URI = Set.of(
            "/arknights/level",
            "/copilot/query"
    );

    @PostConstruct
    public void init() {
        setFilter(new MaaEtagHeaderFilter());
        setUrlPatterns(ETAG_URI);
    }

    private static class MaaEtagHeaderFilter extends ShallowEtagHeaderFilter {

        private static final String CACHE_HEAD = "private, no-cache, max-age=0, must-revalidate";

        @Override
        protected void initFilterBean() {
            // Etag 必须使用弱校验才能与自动压缩兼容
            setWriteWeakETag(true);
        }

        @Override
        protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
                                            int responseStatusCode, InputStream inputStream) {

            if (super.isEligibleForEtag(request, response, responseStatusCode, inputStream)) {
                // 使用 ETag 机制的 URI，若其响应中不存在缓存控制头，则配置默认值
                final String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
                if (cacheControl == null) {
                    response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_HEAD);
                }
                return true;
            }

            return false;
        }
    }
}
