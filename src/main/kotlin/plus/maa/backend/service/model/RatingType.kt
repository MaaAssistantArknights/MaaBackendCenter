package plus.maa.backend.service.model

/**
 * @author LoMu
 * Date  2023-01-22 19:48
 */
enum class RatingType(val display: Int) {
    LIKE(1),
    DISLIKE(2),
    NONE(0);

    companion object {
        /**
         * 将rating转换为  0 = NONE 1 = LIKE 2 = DISLIKE
         *
         * @param type rating
         * @return type
         */
        fun fromRatingType(type: String?): RatingType {
            return when (type) {
                "Like" -> LIKE
                "Dislike" -> DISLIKE
                else -> NONE
            }
        }
    }
}


