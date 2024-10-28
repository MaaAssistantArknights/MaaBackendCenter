package plus.maa.backend.service

import org.junit.jupiter.api.Test
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.service.CopilotService.Companion.getHotScore
import java.time.LocalDateTime

class CopilotServiceTest {
    @Test
    fun testHotScores() {
        val now = LocalDateTime.now()
        val beforeWeek = now.minusDays(8L)
        val copilots = arrayOfNulls<Copilot>(5)
        val lastWeekLikeCounts = LongArray(5)
        val lastWeekDislikeCounts = LongArray(5)
        // 一月前的作业，评分高，但是只有一条近期好评，浏览量高
        val oldGreat = Copilot(doc = Copilot.Doc(title = "test"))
        oldGreat.uploadTime = beforeWeek.minusDays(14)
        oldGreat.views = 20000L
        copilots[0] = oldGreat
        lastWeekLikeCounts[0] = 1L
        lastWeekDislikeCounts[0] = 0L

        // 近期作业，含有差评，但是均为近期评分
        val newGreat = Copilot(doc = Copilot.Doc(title = "test"))
        newGreat.uploadTime = now
        newGreat.views = 1000L
        copilots[1] = newGreat
        lastWeekLikeCounts[1] = 6L
        lastWeekDislikeCounts[1] = 1L

        // 近期作业，差评较多，均为近期评分
        val newBad = Copilot(doc = Copilot.Doc(title = "test"))
        newBad.uploadTime = now
        newBad.views = 500L
        copilots[2] = newBad
        lastWeekLikeCounts[2] = 2L
        lastWeekDislikeCounts[2] = 4L

        // 一月前的作业，评分高，但是只有一条近期好评，浏览量尚可
        val oldNormal = Copilot(doc = Copilot.Doc(title = "test"))
        oldNormal.uploadTime = beforeWeek.minusDays(21L)
        oldNormal.views = 4000L
        copilots[3] = oldNormal
        lastWeekLikeCounts[3] = 1L
        lastWeekDislikeCounts[3] = 0L

        // 新增作业，暂无评分
        val newEmpty = Copilot(doc = Copilot.Doc(title = "test"))
        newEmpty.uploadTime = now
        newEmpty.views = 100L
        copilots[4] = newEmpty
        lastWeekLikeCounts[4] = 0L
        lastWeekDislikeCounts[4] = 0L

        for (i in 0..4) {
            copilots[i]!!.hotScore = getHotScore(copilots[i]!!, lastWeekLikeCounts[i], lastWeekDislikeCounts[i])
        }

        // 近期好评 > 远古好评 > 近期新增 > 近期差评 > 远古一般
        check(newGreat.hotScore > oldGreat.hotScore)
        check(newEmpty.hotScore > oldGreat.hotScore)
        check(oldGreat.hotScore > newBad.hotScore)
        check(oldNormal.hotScore > newBad.hotScore)
    }
}
