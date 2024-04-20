package plus.maa.backend.service.sensitiveword

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
    fun <T> validate(value: T) {
        if (value == null) return
        val text = if (value is String) value else objectMapper.writeValueAsString(value)
        val detected = wordTree.matchAll(text)
        if (detected.size > 0) throw SensitiveWordException("包含敏感词：$detected")
    }
}
