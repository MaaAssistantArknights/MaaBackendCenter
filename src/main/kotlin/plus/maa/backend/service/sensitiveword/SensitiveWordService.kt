package plus.maa.backend.service.sensitiveword

import cn.hutool.dfa.WordTree
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.config.external.SensitiveWord

@Service
class SensitiveWordService(
    private val ctx: ApplicationContext,
    maaCopilotProperties: MaaCopilotProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}
    private val wordTree = WordTree().apply {
        val path = maaCopilotProperties.sensitiveWord.path
        try {
            ctx.getResource(path).inputStream.bufferedReader().use { it.lines().forEach(::addWord) }
            log.info { "初始化敏感词库完成: $path" }
        } catch (e: Exception) {
            log.error { "初始化敏感词库失败: $path" }
            throw e
        }
    }

    @Throws(SensitiveWordException::class)
    fun <T> validate(value: T, sensitiveWord: SensitiveWord) {
        if (value == null) return

        // 将输入转换为字符串
        val text = if (value is String) value else objectMapper.writeValueAsString(value)

        // 使用白名单正则表达式移除文本中匹配的部分
        var sanitizedText = text
        sensitiveWord.whitelistPath?.let { path ->
            // 加载白名单正则表达式列表
            val whitelistPatterns = loadWhitelistPatterns(path)
            whitelistPatterns.forEach { pattern ->
                sanitizedText = sanitizedText.replace(Regex(pattern), "")
            }
        }

        // 使用处理后的文本进行敏感词匹配
        val detected = wordTree.matchAll(sanitizedText)
        if (detected.isNotEmpty()) throw SensitiveWordException("包含敏感词：$detected")
    }

    /**
     * 从白名单文件中加载正则表达式，每一行一个正则表达式，空行忽略
     */
    fun loadWhitelistPatterns(path: String): List<String> {
        // 从 classpath 中读取文件内容（根据项目实际情况调整文件读取方式）
        val whitelistFile = object {}.javaClass.classLoader.getResource(path)?.readText() ?: ""
        return whitelistFile.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}
