package plus.maa.backend.config.external;

@lombok.Data
public class Jwt {
    /**
     * Header name
     */
    private String header;
    /**
     * 默认的JwtToken过期时间，以秒为单位
     */
    private long expire = 21600;
    /**
     * JwtToken的加密密钥
     */
    private String secret;
}