package plus.maa.backend.config.external;

@lombok.Data
public class Vcode {
    /**
     * 默认的验证码失效时间,以秒为单位
     */
    private long expire;
}