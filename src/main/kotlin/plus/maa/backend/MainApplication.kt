package plus.maa.backend

import com.kotlinorm.orm.insert.InsertClause.Companion.execute
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import io.github.oshai.kotlinlogging.KotlinLogging
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
import plus.maa.backend.repository.entity.CopilotEntity
import plus.maa.backend.repository.entity.OperatorEntity
import plus.maa.backend.repository.entity.UserEntity
import plus.maa.backend.service.model.CommentStatus

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
    val userRepository: UserRepository,
    val copilotRepository: CopilotRepository,
) {
    private val log = KotlinLogging.logger { }

    @EventListener(ApplicationReadyEvent::class)
    fun doMigration() {
        migrateUser()
        migrateCopilot()
    }

    fun migrateUser() {
        val exists = UserEntity().select().queryOneOrNull()
        if (exists != null) {
            // 已经完成迁移
            log.info { "用户对象已经完成迁移" }
            return
        }
        val users = userRepository.findAllBy()
        log.info { "迁移用户" }
        var migratedSize = 0
        val chunkList = ArrayList<UserEntity>()

        users.forEach { user ->
            if (chunkList.size == 400) {
                migratedSize += 400
                log.info { "迁移用户，当前处理：${migratedSize}" }
                chunkList.insert().execute()
                chunkList.clear()
            }
            chunkList.add(
                UserEntity(
                    userId = user.userId,
                    userName = user.userName,
                    email = user.email,
                    password = user.password,
                    pwdUpdateTime = user.pwdUpdateTime,
                    status = user.status,
                    followingCount = user.followingCount,
                    fansCount = user.fansCount
                )
            )
        }
        // 插入剩余数据
        chunkList.insert().execute()
    }

    fun migrateCopilot() {
        val exists = CopilotEntity().select().queryOneOrNull()
        if (exists != null) {
            log.info { "作业已经全部完成迁移" }
            return
        }
        val copilots = copilotRepository.findByContentIsNotNull()
        log.info { "开始迁移作业列表" }
        var migratedSize = 0
        val chunkList = ArrayList<Pair<CopilotEntity, List<OperatorEntity>?>>()
        copilots.forEach { copilot ->
            if (chunkList.size == 400) {
                migratedSize += 400
                log.info { "迁移作业列表，当前处理：${migratedSize}" }
                chunkList.map { it.first }.insert().execute()
                chunkList.mapNotNull { it.second }
                    .flatMap { it }.insert().execute()
                chunkList.clear()
            }
            chunkList.add(
                CopilotEntity(
                    copilotId = copilot.copilotId,
                    stageName = copilot.stageName,
                    uploaderId = copilot.uploaderId,
                    views = copilot.views,
                    ratingLevel = copilot.ratingLevel,
                    ratingRatio = copilot.ratingRatio,
                    likeCount = copilot.likeCount,
                    dislikeCount = copilot.dislikeCount,
                    hotScore = copilot.hotScore,
                    title = copilot.doc?.title,
                    details = copilot.doc?.details,
                    firstUploadTime = copilot.firstUploadTime,
                    uploadTime = copilot.uploadTime,
                    content = copilot.content,
                    status = copilot.status,
                    commentStatus = copilot.commentStatus ?: CommentStatus.ENABLED,
                    delete = copilot.delete,
                    deleteTime = copilot.deleteTime,
                    notification = copilot.notification
                ) to copilot.opers?.map { oper ->
                    OperatorEntity(
                        copilotId = copilot.copilotId,
                        name = oper.name
                    )
                })
        }
        chunkList.map { it.first }.insert().execute()
        chunkList.mapNotNull { it.second }
            .flatMap { it }.insert().execute()
        log.info { "migration successfully" }
    }

}
