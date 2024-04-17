package plus.maa.backend.task

import io.mockk.every
import io.mockk.mockk
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import plus.maa.backend.repository.CopilotRepository
import plus.maa.backend.repository.RedisCache
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.Rating
import plus.maa.backend.service.level.ArkLevelService
import plus.maa.backend.service.model.RatingCount
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CopilotScoreRefreshTaskTest {
    private val copilotRepository = mockk<CopilotRepository>()
    private val mongoTemplate = mockk<MongoTemplate>()
    private val redisCache = mockk<RedisCache>()
    private val arkLevelService = mockk<ArkLevelService>()
    private val refreshTask: CopilotScoreRefreshTask = CopilotScoreRefreshTask(
        arkLevelService,
        redisCache,
        copilotRepository,
        mongoTemplate,
    )

    @Test
    fun testRefreshScores() {
        val now = LocalDateTime.now()
        val copilot1 = Copilot(doc = Copilot.Doc(title = "test"))
        copilot1.copilotId = 1L
        copilot1.views = 100L
        copilot1.uploadTime = now
        copilot1.stageName = "stage1"
        val copilot2 = Copilot(doc = Copilot.Doc(title = "test"))
        copilot2.copilotId = 2L
        copilot2.views = 200L
        copilot2.uploadTime = now
        copilot2.stageName = "stage2"
        val copilot3 = Copilot(doc = Copilot.Doc(title = "test"))
        copilot3.copilotId = 3L
        copilot3.views = 200L
        copilot3.uploadTime = now
        copilot3.stageName = "stage3"
        val allCopilots = listOf(copilot1, copilot2, copilot3)

        // 配置copilotRepository
        every {
            copilotRepository.findAllByDeleteIsFalse(any<Pageable>())
        } returns PageImpl(allCopilots)

        // 配置mongoTemplate
        every {
            mongoTemplate.aggregate(any(), Rating::class.java, RatingCount::class.java)
        } returns AggregationResults(
            listOf(
                RatingCount("1", 1L),
                RatingCount("2", 0L),
                RatingCount("3", 0L),
            ),
            Document(),
        )

        val arkLevel = ArkLevel()
        arkLevel.isOpen = true
        arkLevel.closeTime = LocalDateTime.now().plus(1, ChronoUnit.DAYS)
        every { arkLevelService.findByLevelIdFuzzy(any()) } returns arkLevel
        every { copilotRepository.saveAll(any<Iterable<Copilot>>()) } returns allCopilots
        every { redisCache.syncRemoveCacheByPattern(any()) } returns Unit
        refreshTask.refreshHotScores()

        check(copilot1.hotScore > 0)
        check(copilot2.hotScore > 0)
    }

    @Test
    fun testRefreshTop100HotScores() {
        val now = LocalDateTime.now()
        val copilot1 = Copilot(doc = Copilot.Doc(title = "test"))
        copilot1.copilotId = 1L
        copilot1.views = 100L
        copilot1.uploadTime = now
        copilot1.stageName = "stage1"
        val copilot2 = Copilot(doc = Copilot.Doc(title = "test"))
        copilot2.copilotId = 2L
        copilot2.views = 200L
        copilot2.uploadTime = now
        copilot2.stageName = "stage2"
        val copilot3 = Copilot(doc = Copilot.Doc(title = "test"))
        copilot3.copilotId = 3L
        copilot3.views = 200L
        copilot3.uploadTime = now
        copilot3.stageName = "stage3"
        val allCopilots = listOf(copilot1, copilot2, copilot3)

        // 配置 RedisCache
        every { redisCache.getZSetReverse("rate:hot:copilotIds", 0, 99) } returns setOf("1", "2", "3")

        // 配置copilotRepository
        every {
            copilotRepository.findByCopilotIdInAndDeleteIsFalse(any())
        } returns allCopilots

        // 配置mongoTemplate
        every {
            mongoTemplate.aggregate(any(), Rating::class.java, RatingCount::class.java)
        } returns AggregationResults(
            listOf(
                RatingCount("1", 1L),
                RatingCount("2", 0L),
                RatingCount("3", 0L),
            ),
            Document(),
        )
        val arkLevel = ArkLevel()
        arkLevel.isOpen = true
        arkLevel.closeTime = LocalDateTime.now().plus(1, ChronoUnit.DAYS)
        every { arkLevelService.findByLevelIdFuzzy(any()) } returns arkLevel
        every { copilotRepository.saveAll(any<Iterable<Copilot>>()) } returns allCopilots
        every { redisCache.removeCache(any<String>()) } returns Unit
        every { redisCache.syncRemoveCacheByPattern(any()) } returns Unit
        refreshTask.refreshTop100HotScores()

        check(copilot1.hotScore > 0)
        check(copilot2.hotScore > 0)
    }
}
