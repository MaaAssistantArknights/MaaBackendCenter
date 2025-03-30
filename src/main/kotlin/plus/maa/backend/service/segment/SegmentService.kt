package plus.maa.backend.service.segment

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.util.Pair
import org.springframework.stereotype.Service
import org.wltea.analyzer.cfg.Configuration
import org.wltea.analyzer.cfg.DefaultConfig
import org.wltea.analyzer.core.IKSegmenter
import org.wltea.analyzer.dic.Dictionary
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.repository.entity.Copilot
import java.io.StringReader
import kotlin.math.ceil

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
        return set.toList()
    }

    override fun afterPropertiesSet() {
        log.info { "update segments start" }
        val size = 1000
        val totalQuery = Query()
        if (!properties.segmentInfo.updateFullIndex) {
            totalQuery.addCriteria(Criteria.where("segment").exists(false))
        }
        val total = mongoTemplate.count<Copilot>(totalQuery)
        val num = if (total == 0L) 0 else ceil(total.toDouble() / size).toInt()
        val count = (0 until num).sumOf { i ->
            val q = Query().apply {
                with(PageRequest.of(i, size))
                fields().include("doc")
                if (!properties.segmentInfo.updateFullIndex) {
                    addCriteria(Criteria.where("segment").exists(false))
                }
            }
            val result = mongoTemplate.find(q, Copilot::class.java)
            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Copilot::class.java)

            result.map {
                val segments = getSegment(it.doc?.title, it.doc?.details).joinToString(separator = " ")
                Update().set("segment", segments).run {
                    Pair.of<Query, UpdateDefinition>(Query().addCriteria(Criteria.where("_id").`is`(it.id)), this)
                }
            }.let {
                ops.updateMulti(it).execute().matchedCount
            }
        }
        log.info { "update segments $count" }
    }
}
