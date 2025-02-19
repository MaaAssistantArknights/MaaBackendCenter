package plus.maa.backend.config.external

data class SensitiveWord(
    /**
     * 敏感词文件路径，默认为 `classpath:sensitive-word.txt`
     */
    var path: String = "classpath:sensitive-word.txt",

    /**
     * 白名单文件路径，每一行是一个正则表达式，匹配到的内容将从文本中移除
     */
    var whitelistPath: String = "classpath:sensitive-word-whitelist.txt",
)
