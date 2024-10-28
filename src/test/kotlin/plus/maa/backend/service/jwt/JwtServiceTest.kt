package plus.maa.backend.service.jwt

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import plus.maa.backend.config.external.Jwt
import plus.maa.backend.config.external.MaaCopilotProperties

class JwtServiceTest {
    private fun createService(): JwtService {
        val properties = MaaCopilotProperties()
        val jwtSettings = Jwt()
        jwtSettings.secret = "whatever you want"
        properties.jwt = jwtSettings

        return JwtService(properties)
    }

    @Test
    @Throws(JwtExpiredException::class, JwtInvalidException::class)
    fun authTokenCodec() {
        val service = createService()
        val subject = "some user id"
        val jwtId = "some jwt Id"

        val token = service.issueAuthToken(subject, jwtId, ArrayList())
        val parsedToken = service.verifyAndParseAuthToken(token.value)

        check(subject == parsedToken.subject)
        check(jwtId == parsedToken.jwtId)
        check(parsedToken.isAuthenticated)
    }

    @Test
    fun wrongTypeParseShouldFail() {
        val service = createService()
        val authToken = service.issueAuthToken("some user id", null, ArrayList())
        assertThrows<JwtInvalidException> {
            service.verifyAndParseRefreshToken(authToken.value)
        }
        val refreshToken = service.issueRefreshToken("some user id", null)
        assertThrows<JwtInvalidException> {
            service.verifyAndParseAuthToken(refreshToken.value)
        }
    }
}
