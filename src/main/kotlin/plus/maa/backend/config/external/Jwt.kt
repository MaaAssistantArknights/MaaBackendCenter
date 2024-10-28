package plus.maa.backend.config.external

data class Jwt(
    /**
     * Header name
     */
    var header: String = "Authorization",
    /**
     * 默认的JwtToken过期时间，以秒为单位
     */
    var expire: Long = 21600,
    /*
     * 默认的 Refresh Token 过期时间，以秒为单位
     */
    var refreshExpire: Long = (30 * 24 * 60 * 60).toLong(),
    /**
     * JwtToken的加密密钥
     */
    var secret: String = "",
)
