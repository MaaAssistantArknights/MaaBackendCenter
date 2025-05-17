package plus.maa.backend.service.segment

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import org.wltea.analyzer.cfg.Configuration
import org.wltea.analyzer.cfg.DefaultConfig
import org.wltea.analyzer.core.IKSegmenter
import org.wltea.analyzer.dic.Dictionary
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.repository.entity.Copilot
import java.io.StringReader
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class SegmentService(
    private val ctx: ApplicationContext,
    private val properties: MaaCopilotProperties,
    private val mongoTemplate: MongoTemplate,
) : InitializingBean {

    private val log = KotlinLogging.logger { }

    private val cfg: Configuration = DefaultConfig.getInstance().apply {
        setUseSmart(false)
        Dictionary.initial(this)
        ctx.getResource(properties.segmentInfo.path).inputStream.bufferedReader().use { r ->
            Dictionary.getSingleton().addWords(r.readLines())
        }
    }

    fun getSegment(vararg content: String?): List<String> {
        val set = HashSet<String>()
        content.forEach {
            if (it.isNullOrBlank()) return@forEach
            val helper = IKSegmenter(StringReader(it), cfg)
            while (true) {
                val lex = helper.next() ?: break
                set.add(lex.lexemeText)
            }
        }
        return set.filter {
            it.isNotBlank()
                && it !in properties.segmentInfo.filteredWordInfo
        }
    }

    companion object {
        // copilotId -> segment list
        private val INDEX = ConcurrentHashMap<String, MutableSet<Long>>()
    }

    fun updateIndex(id: Long, vararg content: String?) {
        getSegment(*content).forEach { word ->
            INDEX.computeIfAbsent(word) {
                HashSet()
            }.add(id)
        }
    }

    fun removeIndex(id: Long, vararg content: String?) {
        getSegment(*content).forEach { word ->
            INDEX.get(word)?.remove(id)
        }
    }

    fun fetchIndexInfo(word: String) = INDEX.getOrDefault(word, emptySet())

    override fun afterPropertiesSet() {
        val segUpdateAt = Instant.now()
        log.info { "Segments updating start at: $segUpdateAt" }

        val query = Query().apply {
            fields().include("id", "doc", "copilotId")
            addCriteria(Copilot::delete isEqualTo false)
        }
        // small data, fetch all info
        val fetched = mongoTemplate.find<Copilot>(query)

        fetched.forEach {
            updateIndex(it.copilotId!!, it.doc?.title, it.doc?.details)
        }

        log.info { "Segments updated: ${INDEX.size}" }
    }
}
