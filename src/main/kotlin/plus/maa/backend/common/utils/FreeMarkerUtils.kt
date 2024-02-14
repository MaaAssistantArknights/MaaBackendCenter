package plus.maa.backend.common.utils

import freemarker.template.Configuration
import freemarker.template.TemplateException
import plus.maa.backend.controller.response.MaaResultException
import java.io.IOException
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * @author dragove
 * created on 2023/1/17
 */
object FreeMarkerUtils {
    private val cfg = Configuration(Configuration.VERSION_2_3_32)

    init {
        cfg.setClassForTemplateLoading(FreeMarkerUtils::class.java, "/static/templates/ftlh")
        cfg.setEncoding(Locale.CHINA, StandardCharsets.UTF_8.name())
    }

    fun parseData(dataModel: Any?, templateName: String?): String {
        try {
            val template = cfg.getTemplate(templateName)
            val sw = StringWriter()
            template.process(dataModel, sw)
            return sw.toString()
        } catch (e: IOException) {
            throw MaaResultException("获取freemarker模板失败")
        } catch (e: TemplateException) {
            throw MaaResultException("freemarker模板处理失败")
        }
    }
}
