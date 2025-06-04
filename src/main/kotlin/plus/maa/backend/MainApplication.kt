package plus.maa.backend

import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.stereotype.Component
import plus.maa.backend.repository.UserRepository
import plus.maa.backend.repository.entity.UserEntity

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
    val userRepository: UserRepository
) {
    @EventListener(ApplicationReadyEvent::class)
    fun tryMigrateUser() {
        val exists = UserEntity().select().queryOneOrNull()
        if (exists != null) {
            // 已经完成迁移
            println(exists)
            return
        }

        val users = userRepository.findAll(Pageable.ofSize(1))
        val user = users.toList()[0]
        UserEntity(
            userId = user.userId,
            userName = user.userName,
            email = user.email,
            password = user.password,
            pwdUpdateTime = user.pwdUpdateTime,
            status = user.status,
            followingCount = user.followingCount,
            fansCount = user.fansCount
        ).insert().execute()

    }

}
