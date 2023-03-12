package plus.maa.backend.service.jwt;

public class JwtExpiredException extends Exception {
    public JwtExpiredException(String message) {
        super(message);
    }
}
