package plus.maa.backend.service;

import org.junit.jupiter.api.Test;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CopilotServiceTest {

    @Test
    void testHotScores() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beforeWeek = now.minusDays(8L);

        // 一月前的作业，评分高，但是只有一条近期好评，浏览量高
        Copilot oldGreat = new Copilot();
        oldGreat.setUploadTime(beforeWeek.minusDays(14));
        oldGreat.setViews(20000L);
        CopilotRating rating1 = new CopilotRating();
        rating1.setRatingUsers(List.of(
                new CopilotRating.RatingUser("0", "Like", now),
                new CopilotRating.RatingUser("a", "Like", beforeWeek),
                new CopilotRating.RatingUser("b", "Like", beforeWeek),
                new CopilotRating.RatingUser("c", "Like", beforeWeek)));

        // 近期作业，含有差评，但是均为近期评分
        Copilot newGreat = new Copilot();
        newGreat.setUploadTime(now);
        newGreat.setViews(1000L);
        CopilotRating rating2 = new CopilotRating();
        rating2.setRatingUsers(List.of(new CopilotRating.RatingUser("d", "Like", now),
                new CopilotRating.RatingUser("e", "Like", now),
                new CopilotRating.RatingUser("a", "Like", now),
                new CopilotRating.RatingUser("f", "Like", now),
                new CopilotRating.RatingUser("g", "Like", now),
                new CopilotRating.RatingUser("b", "Dislike", now),
                new CopilotRating.RatingUser("c", "Like", now)));

        // 近期作业，差评较多，均为近期评分
        Copilot newBad = new Copilot();
        newBad.setUploadTime(now);
        newBad.setViews(500L);
        CopilotRating rating3 = new CopilotRating();
        rating3.setRatingUsers(List.of(new CopilotRating.RatingUser("a", "Like", now),
                new CopilotRating.RatingUser("b", "Dislike", now),
                new CopilotRating.RatingUser("c", "Dislike", now),
                new CopilotRating.RatingUser("d", "Dislike", now),
                new CopilotRating.RatingUser("e", "Dislike", now),
                new CopilotRating.RatingUser("f", "Like", now)));

        // 一月前的作业，评分高，但是只有一条近期好评，浏览量尚可
        Copilot oldNormal = new Copilot();
        oldNormal.setUploadTime(beforeWeek.minusDays(21L));
        oldNormal.setViews(4000L);
        CopilotRating rating4 = new CopilotRating();
        rating4.setRatingUsers(List.of(
                new CopilotRating.RatingUser("a", "Like", now),
                new CopilotRating.RatingUser("b", "Like", beforeWeek),
                new CopilotRating.RatingUser("c", "Like", beforeWeek)));

        // 新增作业，暂无评分
        Copilot newEmpty = new Copilot();
        newEmpty.setUploadTime(now);
        newEmpty.setViews(100L);
        CopilotRating rating5 = new CopilotRating();
        rating5.setRatingUsers(List.of());

        List<Tuple2<Copilot, CopilotRating>> orderRatings = List.of(Tuples.of(oldGreat, rating1),
                Tuples.of(newGreat, rating2), Tuples.of(newBad, rating3), Tuples.of(oldNormal, rating4),
                Tuples.of(newEmpty, rating5));

        for (Tuple2<Copilot, CopilotRating> orderRating : orderRatings) {
            orderRating.getT1().setHotScore(CopilotService.getHotScore(orderRating.getT1(), orderRating.getT2()));
        }

        // 近期好评 > 远古好评 > 近期新增 > 近期差评 > 远古一般
        assertTrue(newGreat.getHotScore() > oldGreat.getHotScore());
        assertTrue(newEmpty.getHotScore() > oldGreat.getHotScore());
        assertTrue(oldGreat.getHotScore() > newBad.getHotScore());
        assertTrue(oldNormal.getHotScore() > newBad.getHotScore());
    }

}
