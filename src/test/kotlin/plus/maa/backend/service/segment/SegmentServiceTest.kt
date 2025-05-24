package plus.maa.backend.service.segment

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.set
import org.springframework.data.util.Pair
import plus.maa.backend.repository.entity.Copilot
import java.time.Instant

@SpringBootTest
class SegmentServiceTest {
    @Autowired
    private lateinit var service: SegmentService

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private val log = KotlinLogging.logger { }

    @Test
    fun generateSegments() {
        var count = 0L
        while (true) {
            val fetched = mongoTemplate.find<Copilot>(
                Query().apply {
                    addCriteria(Copilot::delete isEqualTo false)
                    skip(count)
                    limit(20)
                },
            )
            if (fetched.isEmpty()) break

            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Copilot::class.java)
            val updates = fetched.map {
                val newSeg = service.getSegment(it.doc?.title, it.doc?.details).joinToString(" ")
                Pair.of<Query, UpdateDefinition>(
                    Query().addCriteria(Copilot::id isEqualTo it.id),
                    Update().apply {
                        set(Copilot::segment, newSeg)
                        set(Copilot::segmentUpdateAt, Instant.now())
                    },
                )
            }
            count += ops.updateMulti(updates).execute().matchedCount

            log.info { "Fetched ${fetched.size} documents, updated $count documents" }
        }

        log.info { "Segments updated: $count" }
    }
}
