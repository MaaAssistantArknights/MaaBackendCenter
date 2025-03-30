package plus.maa.backend.config.external

data class SegmentInfo(
    var path: String = "classpath:arknights.txt",
    /**
     * 是否全局更新索引
     */
    var forceUpdateAllIndexes: Boolean = false,
    var updateBatchSize: Int = 1000,
)
