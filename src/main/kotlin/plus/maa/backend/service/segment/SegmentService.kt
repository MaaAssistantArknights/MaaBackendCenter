package plus.maa.backend.service.segment

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.set
import org.springframework.data.util.Pair
import org.springframework.stereotype.Service
import org.wltea.analyzer.cfg.Configuration
import org.wltea.analyzer.cfg.DefaultConfig
import org.wltea.analyzer.core.IKSegmenter
import org.wltea.analyzer.dic.Dictionary
import plus.maa.backend.common.extensions.addNorCriteria
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.repository.entity.Copilot
import java.io.StringReader
import java.time.Instant

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
        val segUpdateAt = Instant.now()
        log.info { "Segments updating start at: $segUpdateAt" }

        val query = Query().apply {
            addNorCriteria(Copilot::segmentUpdateAt isEqualTo segUpdateAt)
            if (!properties.segmentInfo.forceUpdateAllIndexes) {
                addCriteria(Copilot::segment exists false)
            }
            fields().include("id").include("doc")
            // NOTICE: do not specify pageNumber because the updated object will not satisfy the criteria anymore
            limit(properties.segmentInfo.updateBatchSize)
        }

        var count = 0
        while (true) {
            val fetched = mongoTemplate.find<Copilot>(query)
            if (fetched.isEmpty()) break

            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Copilot::class.java)
            val updates = fetched.map {
                val newSeg = getSegment(it.doc?.title, it.doc?.details).joinToString(" ")
                Pair.of<Query, UpdateDefinition>(
                    Query().addCriteria(Copilot::id isEqualTo it.id),
                    Update().apply {
                        set(Copilot::segment, newSeg)
                        set(Copilot::segmentUpdateAt, segUpdateAt)
                    },
                )
            }
            count += ops.updateMulti(updates).execute().matchedCount
        }

        log.info { "Segments updated: $count" }
    }
}
