package plus.maa.backend.config.external

data class Vcode(
        /**
         * 默认的验证码失效时间,以秒为单位
         */
        var expire: Long = 0
)