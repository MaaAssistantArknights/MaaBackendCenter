package plus.maa.backend.service

import cn.hutool.extra.mail.MailAccount
import cn.hutool.extra.mail.MailUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service
import plus.maa.backend.common.utils.FreeMarkerUtils
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.repository.RedisCache
import plus.maa.backend.service.model.CommentNotification
import java.util.*

/**
 * @author LoMu
 * Date 2022-12-24 11:05
 */
@Service
class EmailService(
    private val maaCopilotProperties: MaaCopilotProperties,
    @Value("\${debug.email.no-send:false}")
    private val flagNoSend: Boolean = false,
    private val redisCache: RedisCache,
    @Resource(name = "emailTaskExecutor")
    private val emailTaskExecutor: AsyncTaskExecutor
) {
    private val log = KotlinLogging.logger { }
    private val mail = maaCopilotProperties.mail
    private val mailAccount = MailAccount()
        .setHost(mail.host)
        .setPort(mail.port)
        .setFrom(mail.from)
        .setUser(mail.user)
        .setPass(mail.pass)
        .setSslEnable(mail.ssl)
        .setStarttlsEnable(mail.starttls)

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */
    fun sendVCode(email: String) {
        // 一个过期周期最多重发十条，记录已发送的邮箱以及间隔时间
        val timeout = maaCopilotProperties.vcode.expire / 10
        if (!redisCache.setCacheIfAbsent("HasBeenSentVCode:$email", timeout, timeout)) {
            // 设置失败，说明 key 已存在
            throw MaaResultException(403, String.format("发送验证码的请求至少需要间隔 %d 秒", timeout))
        }
        // 执行异步任务
        asyncSendVCode(email)
    }

    private fun asyncSendVCode(email: String) = emailTaskExecutor.execute {
        // 6位随机数验证码
        val vCode = RandomStringUtils.random(6, true, true).uppercase(Locale.getDefault())
        if (flagNoSend) {
            log.warn { "Email not sent, no-send enabled, vcode is $vCode" }
        } else {
            val subject = "Maa Backend Center 验证码"
            val dataModel = mapOf(
                "content" to "mail-vCode.ftlh",
                "obj" to vCode,
            )
            val content = FreeMarkerUtils.parseData("mail-includeHtml.ftlh", dataModel)
            MailUtil.send(mailAccount, listOf(email), subject, content, true)
        }
        // 存redis
        redisCache.setCache("vCodeEmail:$email", vCode, maaCopilotProperties.vcode.expire)
    }

    /**
     * 检验验证码并抛出异常
     * @param email 邮箱
     * @param vcode 验证码
     * @throws MaaResultException 验证码错误
     */
    fun verifyVCode(email: String, vcode: String) {
        if (!redisCache.removeKVIfEquals("vCodeEmail:$email", vcode.uppercase(Locale.getDefault()))) {
            throw MaaResultException(401, "验证码错误")
        }
    }

    fun sendCommentNotification(email: String, commentNotification: CommentNotification) = emailTaskExecutor.execute {
        val limit = 25
        val title = (commentNotification.title ?: "").let {
            if (it.length > limit) it.substring(0, limit - 4) + "...." else it
        }

        val subject = "收到新回复 来自用户@${commentNotification.reName} Re: $title"
        val dataModel = mapOf(
            "content" to "mail-comment-notification.ftlh",
            "authorName" to commentNotification.authorName,
            "frontendLink" to maaCopilotProperties.info.frontendDomain,
            "reName" to commentNotification.reName,
            "date" to commentNotification.date,
            "title" to title,
            "reMessage" to commentNotification.reMessage,
        )
        val content = FreeMarkerUtils.parseData("mail-includeHtml.ftlh", dataModel)

        MailUtil.send(mailAccount, listOf(email), subject, content, true)
    }
}
