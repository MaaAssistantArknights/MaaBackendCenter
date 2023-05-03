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

        Copilot order1 = new Copilot();
        order1.setUploadTime(now);
        order1.setViews(150L);
        CopilotRating rating1 = new CopilotRating();
        rating1.setRatingUsers(List.of(new CopilotRating.RatingUser("0", "Like", now),
                new CopilotRating.RatingUser("a", "Like", now),
                new CopilotRating.RatingUser("b", "Dislike", now),
                new CopilotRating.RatingUser("c", "Like", now)));

        Copilot order2 = new Copilot();
        order2.setUploadTime(beforeWeek);
        order2.setViews(200L);
        CopilotRating rating2 = new CopilotRating();
        rating2.setRatingUsers(List.of(new CopilotRating.RatingUser("a", "Like", now),
                new CopilotRating.RatingUser("b", "Dislike", now),
                new CopilotRating.RatingUser("c", "Like", beforeWeek)));

        Copilot order3 = new Copilot();
        order3.setUploadTime(beforeWeek);
        order3.setViews(1000L);
        CopilotRating rating3 = new CopilotRating();
        rating3.setRatingUsers(List.of(new CopilotRating.RatingUser("a", "Like", beforeWeek),
                new CopilotRating.RatingUser("b", "Dislike", beforeWeek),
                new CopilotRating.RatingUser("c", "Like", beforeWeek)));

        List<Tuple2<Copilot, CopilotRating>> orderRatings = List.of(Tuples.of(order1, rating1),
                Tuples.of(order2, rating2), Tuples.of(order3, rating3));

        for (Tuple2<Copilot, CopilotRating> orderRating : orderRatings) {
            orderRating.getT1().setHotScore(CopilotService.getHotScore(orderRating.getT1(), orderRating.getT2()));
        }

        assertTrue(order1.getHotScore() > order2.getHotScore());
        assertTrue(order2.getHotScore() > order3.getHotScore());
    }

}
