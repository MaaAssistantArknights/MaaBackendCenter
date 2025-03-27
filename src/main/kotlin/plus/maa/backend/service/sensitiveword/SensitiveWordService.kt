package plus.maa.backend.service.sensitiveword

import cn.hutool.dfa.StopChar
import cn.hutool.dfa.WordTree
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import plus.maa.backend.config.external.MaaCopilotProperties

@Service
class SensitiveWordService(
    private val ctx: ApplicationContext,
    maaCopilotProperties: MaaCopilotProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}
    private val wordTree = WordTree().apply {
        StopChar.STOP_WORD.remove('/')
        val path = maaCopilotProperties.sensitiveWord.path
        try {
            ctx.getResource(path).inputStream.bufferedReader().use { it.lines().forEach(::addWord) }
            log.info { "初始化敏感词库完成: $path" }
        } catch (e: Exception) {
            log.error { "初始化敏感词库失败: $path" }
            throw e
        }
    }
    private val whiteList = maaCopilotProperties.sensitiveWord.whitelistPath.let { path ->
        try {
            val list = mutableListOf<String>()
            ctx.getResource(path).inputStream.bufferedReader().useLines(list::addAll)
            val regex = Regex(list.joinToString("|") { "($it)" })
            log.info { "初始化敏感词白名单规则成功: $path" }
            regex
        } catch (e: Exception) {
            log.error { "初始化敏感词白名单规则失败: $path" }
            throw e
        }
    }

    @Throws(SensitiveWordException::class)
    fun <T> validate(value: T) {
        if (value == null) return

        // 将输入转换为字符串
        val text = if (value is String) value else objectMapper.writeValueAsString(value)

        // 使用白名单正则表达式移除文本中匹配的部分
        val sanitizedText = text.replace(whiteList, "")
        // 使用处理后的文本进行敏感词匹配
        val detected = wordTree.matchAll(sanitizedText)
        if (detected.isNotEmpty()) throw SensitiveWordException("包含敏感词：$detected")
    }
}
