package plus.maa.backend.service.model;

import lombok.Getter;


/**
 * @author LoMu
 * Date  2023-01-22 19:48
 */
@Getter
public enum RatingType {

    LIKE(1),
    DISLIKE(2),
    NONE(0);

    private final int display;

    RatingType(int display) {
        this.display = display;
    }

    public static RatingType fromRatingType(String type) {
        return switch (type) {
            case "Like" -> LIKE;
            case "Dislike" -> DISLIKE;
            default -> NONE;
        };

    }
}


