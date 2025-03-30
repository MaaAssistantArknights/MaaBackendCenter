package plus.maa.backend.service.segment

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.wltea.analyzer.cfg.Configuration
import org.wltea.analyzer.cfg.DefaultConfig
import org.wltea.analyzer.core.IKSegmenter
import org.wltea.analyzer.dic.Dictionary
import plus.maa.backend.config.external.MaaCopilotProperties
import java.io.StringReader

@Service
class SegmentService(
    private val ctx: ApplicationContext,
    private val properties: MaaCopilotProperties,
) {

    private val cfg: Configuration = run {
        DefaultConfig.getInstance().also {
            Dictionary.initial(it)
            ctx.getResource(properties.segmentInfo.path).inputStream.bufferedReader().use { r ->
                Dictionary.getSingleton().addWords(r.readLines())
            }
            it.setUseSmart(false)
        }
    }

    fun getSegment(vararg content: String?): List<String> {
        when {
            content.isEmpty() -> return emptyList()
            content.size == 1 -> {
                val helper = IKSegmenter(StringReader(content[0] ?: return emptyList()), cfg)
                val words = mutableListOf<String>()
                while (true) {
                    val lex = helper.next() ?: break
                    words.add(lex.lexemeText)
                }
                return words
            }

            else -> {
                val set = HashSet<String>()
                content.forEach {
                    if (it.isNullOrBlank()) return@forEach
                    val helper = IKSegmenter(StringReader(it), cfg)
                    while (true) {
                        val lex = helper.next() ?: break
                        set.add(lex.lexemeText)
                    }
                }
                return set.toList()
            }
        }
    }
}
