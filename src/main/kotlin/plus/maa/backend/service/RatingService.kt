package plus.maa.backend.service

import org.springframework.stereotype.Service
import plus.maa.backend.repository.RatingRepository
import plus.maa.backend.repository.entity.Rating
import plus.maa.backend.service.model.RatingType
import java.time.LocalDateTime

@Service
class RatingService(private val ratingRepository: RatingRepository) {
    /**
     * Update rating of target object
     *
     * @param keyType Target key type
     * @param key Key
     * @param raterId Rater's ID
     * @param ratingType Target rating type
     * @return A pair, previous one and the target one.
     */
    fun rate(keyType: Rating.KeyType, key: String, raterId: String, ratingType: RatingType): Pair<Rating, Rating> {
        val rating = ratingRepository.findByTypeAndKeyAndUserId(
            keyType,
            key,
            raterId,
        ) ?: Rating(
            null,
            keyType,
            key,
            raterId,
            RatingType.NONE,
            LocalDateTime.now(),
        )

        if (ratingType == rating.rating) return rating to rating

        val prev = rating.copy()
        rating.rating = ratingType
        rating.rateTime = LocalDateTime.now()
        ratingRepository.save(rating)
        return prev to rating
    }

    /**
     * Calculate like/dislike counts from rating change.
     * @param ratingChange Pair of previous rating and current rating
     * @return Pair of like count change and dislike count change
     */
    fun calcLikeChange(ratingChange: Pair<Rating, Rating>): Pair<Long, Long> {
        val (prev, next) = ratingChange
        val likeCountChange = next.rating.countLike() - prev.rating.countLike()
        val dislikeCountChange = next.rating.countDislike() - prev.rating.countDislike()
        return likeCountChange to dislikeCountChange
    }

    fun rateComment(commentId: String, raterId: String, ratingType: RatingType): Pair<Rating, Rating> =
        rate(Rating.KeyType.COMMENT, commentId, raterId, ratingType)

    fun rateCopilot(copilotId: Long, raterId: String, ratingType: RatingType): Pair<Rating, Rating> =
        rate(Rating.KeyType.COPILOT, copilotId.toString(), raterId, ratingType)

    fun findPersonalRatingOfCopilot(raterId: String, copilotId: Long) =
        ratingRepository.findByTypeAndKeyAndUserId(Rating.KeyType.COPILOT, copilotId.toString(), raterId)
}
