package plus.maa.backend.config.external

data class SegmentInfo(
    var path: String = "classpath:arknights.txt",
    /**
     * 是否全局更新索引
     */
    var updateFullIndex: Boolean = false,
)
