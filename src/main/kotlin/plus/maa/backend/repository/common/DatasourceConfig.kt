package plus.maa.backend.repository.common

import com.kotlinorm.Kronos
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.transformers.TransformerManager.registerValueTransformer
import com.kotlinorm.interfaces.KronosSerializeProcessor
import com.kotlinorm.interfaces.ValueTransformer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import plus.maa.backend.service.model.CommentStatus
import plus.maa.backend.service.model.CopilotSetStatus
import java.time.ZoneId
import kotlin.reflect.KClass

object EnumTransformer : ValueTransformer {
    val mapping = mapOf<String, (String) -> Any>(
        CopilotSetStatus::class.qualifiedName!! to { name -> CopilotSetStatus.valueOf(name) },
        CommentStatus::class.qualifiedName!! to { name -> CommentStatus.valueOf(name) },
    )

    override fun isMatch(
        targetKotlinType: String,
        superTypesOfValue: List<String>,
        kClassOfValue: KClass<*>
    ): Boolean {
        return kClassOfValue == String::class && mapping.keys.contains(targetKotlinType)
    }

    override fun transform(
        targetKotlinType: String,
        value: Any,
        superTypesOfValue: List<String>,
        dateTimeFormat: String?,
        kClassOfValue: KClass<*>
    ): Any = mapping[targetKotlinType]!!.invoke(value as String)
}

object EnumProcessor : KronosSerializeProcessor {
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        val jClass = kClass.java
        if (jClass.isEnum) {
            return kClass.java.enumConstants.first { (it as Enum<*>).name == serializedStr }
        }
        return serializedStr
    }

    override fun serialize(obj: Any): String {
        if (obj is Enum<*>) {
            return obj.name
        }
        return obj.toString()
    }
}


@Configuration
class DataSourceConfig : InitializingBean {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    fun dataSource(): HikariConfig = HikariConfig()
    override fun afterPropertiesSet() {
        val wrapper by lazy {
            KronosBasicWrapper(HikariDataSource(dataSource()))
        }
        Kronos.init {
            registerValueTransformer(EnumTransformer)
            serializeProcessor = EnumProcessor
            tableNamingStrategy = lineHumpNamingStrategy
            fieldNamingStrategy = lineHumpNamingStrategy
            timeZone = ZoneId.systemDefault()
            dataSource = { wrapper }
        }
    }

}
