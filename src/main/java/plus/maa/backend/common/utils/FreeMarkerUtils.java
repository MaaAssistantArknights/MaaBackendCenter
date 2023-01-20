package plus.maa.backend.common.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import plus.maa.backend.controller.response.MaaResultException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * @author dragove
 * created on 2023/1/17
 */
public class FreeMarkerUtils {

    private static final Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
    static {
        cfg.setClassForTemplateLoading(FreeMarkerUtils.class, "/static/templates");
        cfg.setEncoding(Locale.CHINA, StandardCharsets.UTF_8.name());
    }

    public static String parseData(Object dataModel, String templateName) {
        try {
            Template template = cfg.getTemplate(templateName);
            StringWriter sw = new StringWriter();
            template.process(dataModel, sw);
            return sw.toString();
        } catch (IOException e) {
            throw new MaaResultException("获取freemarker模板失败");
        } catch (TemplateException e) {
            throw new MaaResultException("freemarker模板处理失败");
        }
    }

}
