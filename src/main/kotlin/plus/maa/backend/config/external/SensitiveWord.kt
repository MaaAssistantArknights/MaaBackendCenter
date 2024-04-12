package plus.maa.backend.config.external

data class SensitiveWord(
    /**
     * 敏感词文件路径，默认为 `classpath:sensitive-word.txt`
     */
    var path: String = "classpath:sensitive-word.txt",
)
