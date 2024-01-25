package plus.maa.backend.config.external


data class Copilot(
        /**
         * 作业评分总数少于指定值时显示评分不足
         *
         *
         * 默认值：50
         */
        var minValueShowNotEnoughRating: Int = 50
)
