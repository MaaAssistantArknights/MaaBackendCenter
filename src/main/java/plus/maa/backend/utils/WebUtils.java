package plus.maa.backend.utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author AnselYuki
 */
public class WebUtils {
    public static void renderString(HttpServletResponse response, String json, int code) {
        try {
            response.setStatus(code);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
