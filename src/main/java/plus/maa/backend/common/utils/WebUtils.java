package plus.maa.backend.common.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author AnselYuki
 */
@Slf4j
public class WebUtils {
    public static void renderString(HttpServletResponse response, String json, int code) {
        try {
            response.setStatus(code);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().println(json);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
