package plus.maa.backend

import com.kotlinorm.orm.insert.insert
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.stereotype.Component
import plus.maa.backend.repository.CopilotRepository
import plus.maa.backend.repository.UserRepository
import plus.maa.backend.repository.entity.UserEntity
import java.sql.Timestamp

@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableMethodSecurity
class MainApplication

fun main(args: Array<String>) {
    runApplication<MainApplication>(*args)
}

@Component
class DataMigration(
    val copilotRepository: CopilotRepository,
    val userRepository: UserRepository
) {
    @EventListener(ApplicationReadyEvent::class)
    fun tryMigrateUser() {
        val user = userRepository.findByUserId("6828bb1d8ad2ac5001530806")
        val newUser = UserEntity(
            userId = user?.userId,
            userName = user?.userName,
            email = user?.email,
            password = user?.password,
            status = user?.status,
            // TODO use Instant
            pwdUpdateTime = user?.pwdUpdateTime?.toEpochMilli()?.let { Timestamp(it) },
            followingCount = user?.followingCount,
            fansCount = user?.fansCount
        )
        newUser.insert().execute()
    }

}
