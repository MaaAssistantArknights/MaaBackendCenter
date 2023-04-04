package plus.maa.backend.service.jwt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import plus.maa.backend.config.external.Jwt;
import plus.maa.backend.config.external.MaaCopilotProperties;

import java.util.ArrayList;

class JwtServiceTest {

    JwtService createService() {
        var properties = new MaaCopilotProperties();
        var jwtSettings = new Jwt();
        jwtSettings.setSecret("whatever you want");
        properties.setJwt(jwtSettings);

        return new JwtService(properties);
    }

    @Test
    void authTokenCodec() throws JwtExpiredException, JwtInvalidException {
        var service = createService();
        var subject = "some user id";
        var jwtId = "some jwt Id";

        var token = service.issueAuthToken(subject, jwtId, new ArrayList<>());
        var parsedToken = service.verifyAndParseAuthToken(token.getValue());

        assert subject.equals(parsedToken.getSubject());
        assert jwtId.equals(parsedToken.getJwtId());
        assert parsedToken.isAuthenticated();
    }

    @Test
    void refreshTokenCodec() throws JwtExpiredException, JwtInvalidException {
        var service = createService();

        var subject = "some user id";
        var origin = service.issueRefreshToken(subject, null);

        var parsedToken = service.verifyAndParseRefreshToken(origin.getValue());
        assert subject.equals(parsedToken.getSubject());

        var newToken = service.newRefreshToken(parsedToken, null);
        assert !newToken.getIssuedAt().before(parsedToken.getIssuedAt());
        assert !newToken.getNotBefore().before(parsedToken.getNotBefore());
        assert newToken.getExpiresAt().equals(parsedToken.getExpiresAt());
    }

    @Test
    void wrongTypeParseShouldFail() {
        var service = createService();
        var authToken = service.issueAuthToken("some user id", null, new ArrayList<>());
        Assertions.assertThrows(JwtInvalidException.class, () -> service.verifyAndParseRefreshToken(authToken.getValue()));
        var refreshToken = service.issueRefreshToken("some user id", null);
        Assertions.assertThrows(JwtInvalidException.class, () -> service.verifyAndParseAuthToken(refreshToken.getValue()));
    }

}