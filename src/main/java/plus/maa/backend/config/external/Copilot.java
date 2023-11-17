package plus.maa.backend.config.external;

import lombok.Data;

@Data
public class Copilot {

    /**
     * 作业评分总数少于指定值时显示评分不足
     */
    private int minValueShowNotEnoughRating = 100;
}
