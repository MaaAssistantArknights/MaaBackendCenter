package plus.maa.backend.service;

import org.junit.jupiter.api.Test;
import plus.maa.backend.repository.entity.Copilot;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CopilotServiceTest {

    @Test
    void testHotScores() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beforeWeek = now.minusDays(8L);
        Copilot[] copilots = new Copilot[5];
        long[] lastWeekLikeCounts = new long[5];
        long[] lastWeekDislikeCounts = new long[5];
        // 一月前的作业，评分高，但是只有一条近期好评，浏览量高
        Copilot oldGreat = new Copilot();
        oldGreat.setUploadTime(beforeWeek.minusDays(14));
        oldGreat.setViews(20000L);
        copilots[0] = oldGreat;
        lastWeekLikeCounts[0] = 1L;
        lastWeekDislikeCounts[0] = 0L;

        // 近期作业，含有差评，但是均为近期评分
        Copilot newGreat = new Copilot();
        newGreat.setUploadTime(now);
        newGreat.setViews(1000L);
        copilots[1] = newGreat;
        lastWeekLikeCounts[1] = 6L;
        lastWeekDislikeCounts[1] = 1L;


        // 近期作业，差评较多，均为近期评分
        Copilot newBad = new Copilot();
        newBad.setUploadTime(now);
        newBad.setViews(500L);
        copilots[2] = newBad;
        lastWeekLikeCounts[2] = 2L;
        lastWeekDislikeCounts[2] = 4L;


        // 一月前的作业，评分高，但是只有一条近期好评，浏览量尚可
        Copilot oldNormal = new Copilot();
        oldNormal.setUploadTime(beforeWeek.minusDays(21L));
        oldNormal.setViews(4000L);
        copilots[3] = oldNormal;
        lastWeekLikeCounts[3] = 1L;
        lastWeekDislikeCounts[3] = 0L;


        // 新增作业，暂无评分
        Copilot newEmpty = new Copilot();
        newEmpty.setUploadTime(now);
        newEmpty.setViews(100L);
        copilots[4] = newEmpty;
        lastWeekLikeCounts[4] = 0L;
        lastWeekDislikeCounts[4] = 0L;

        for (int i = 0; i < 5; i++) {
            copilots[i].setHotScore(CopilotService.getHotScore(copilots[i], lastWeekLikeCounts[i], lastWeekDislikeCounts[i]));
        }

        // 近期好评 > 远古好评 > 近期新增 > 近期差评 > 远古一般
        assertTrue(newGreat.getHotScore() > oldGreat.getHotScore());
        assertTrue(newEmpty.getHotScore() > oldGreat.getHotScore());
        assertTrue(oldGreat.getHotScore() > newBad.getHotScore());
        assertTrue(oldNormal.getHotScore() > newBad.getHotScore());
    }

}
