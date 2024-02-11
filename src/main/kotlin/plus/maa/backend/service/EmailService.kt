package plus.maa.backend.service

import cn.hutool.extra.mail.MailAccount
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.apache.commons.lang3.RandomStringUtils
import org.apache.logging.log4j.util.Strings
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import plus.maa.backend.common.bo.EmailBusinessObject
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.repository.RedisCache
import plus.maa.backend.service.model.CommentNotification
import java.util.*

private val log = KotlinLogging.logger { }

/**
 * @author LoMu
 * Date 2022-12-24 11:05
 */
@Service
class EmailService(
    @Value("\${maa-copilot.vcode.expire:600}")
    private val expire: Int,
    @Value("\${maa-copilot.info.domain}")
    private val domain: String,
    private val maaCopilotProperties: MaaCopilotProperties,
    @Value("\${debug.email.no-send:false}")
    private val flagNoSend: Boolean = false,
    private val redisCache: RedisCache,
    @Resource(name = "emailTaskExecutor")
    private val emailTaskExecutor: AsyncTaskExecutor
) {

    private val mainAccount = MailAccount()


    /**
     * 初始化邮件账户信息
     */
    @PostConstruct
    private fun initMailAccount() {
        val mail = maaCopilotProperties.mail
        mainAccount
            .setHost(mail.host)
            .setPort(mail.port)
            .setFrom(mail.from)
            .setUser(mail.user)
            .setPass(mail.pass)
            .setSslEnable(mail.ssl)
            .setStarttlsEnable(mail.starttls)

        log.info { "邮件账户信息初始化完成: $mainAccount" }
    }

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */
    fun sendVCode(email: String) {
        // 一个过期周期最多重发十条，记录已发送的邮箱以及间隔时间
        val timeout = expire / 10
        if (!redisCache.setCacheIfAbsent("HasBeenSentVCode:$email", timeout, timeout.toLong())) {
            // 设置失败，说明 key 已存在
            throw MaaResultException(403, String.format("发送验证码的请求至少需要间隔 %d 秒", timeout))
        }
        // 执行异步任务
        asyncSendVCode(email)
    }

    private fun asyncSendVCode(email: String) {
        emailTaskExecutor.execute {
            // 6位随机数验证码
            val vcode = RandomStringUtils.random(6, true, true).uppercase(Locale.getDefault())
            if (flagNoSend) {
                log.debug { "vcode is $vcode" }
                log.warn { "Email not sent, no-send enabled" }
            } else {
                EmailBusinessObject(
                    mailAccount = mainAccount
                ).setEmail(email).sendVerificationCodeMessage(vcode)
            }
            // 存redis
            redisCache.setCache("vCodeEmail:$email", vcode, expire.toLong())
        }
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

    @Async("emailTaskExecutor")
    fun sendCommentNotification(email: String, commentNotification: CommentNotification) {
        val limit = 25

        var title = commentNotification.title
        if (Strings.isNotBlank(title)) {
            if (title.length > limit) {
                title = title.substring(0, limit) + "...."
            }
        }

        val map: MutableMap<String, String> = HashMap()
        map["authorName"] = commentNotification.authorName
        map["forntEndLink"] = maaCopilotProperties.info.frontendDomain
        map["reName"] = commentNotification.reName
        map["date"] = commentNotification.date
        map["title"] = title
        map["reMessage"] = commentNotification.reMessage
        EmailBusinessObject(
            mailAccount = mainAccount,
            title = "收到新回复 来自用户@" + commentNotification.reName + " Re: " + map["title"]
        ).setEmail(email).sendCommentNotification(map)
    }
}
