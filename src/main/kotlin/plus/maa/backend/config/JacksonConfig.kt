package plus.maa.backend.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.io.IOException
import java.time.*
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfig(private val jacksonProperties: JacksonProperties) {
    @Bean
    fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            val format = jacksonProperties.dateFormat
            val timeZone = jacksonProperties.timeZone
            val formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId())
            builder.serializers(LocalDateTimeSerializer(formatter))
        }
    }

    class LocalDateTimeSerializer(private val formatter: DateTimeFormatter) :
        StdSerializer<LocalDateTime>(LocalDateTime::class.java) {
        @Throws(IOException::class)
        override fun serialize(value: LocalDateTime, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(value.atZone(ZoneId.systemDefault()).format(formatter))
        }
    }
}