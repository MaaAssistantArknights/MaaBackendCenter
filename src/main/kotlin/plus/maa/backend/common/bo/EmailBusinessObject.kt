package plus.maa.backend.common.bo

import cn.hutool.extra.mail.MailAccount
import cn.hutool.extra.mail.MailUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import plus.maa.backend.common.utils.FreeMarkerUtils
import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.set

private val log = KotlinLogging.logger {  }

/**
 * @author LoMu
 * Date 2022-12-23 23:57
 */
class EmailBusinessObject(
    // 发件人信息
    private val mailAccount: MailAccount,
    // 自定义标题
    private val title: String = DEFAULT_TITLE_PREFIX,
    // 邮件内容
    private val message: String? = null,
    // html标签是否被识别使用
    private val isHtml: Boolean = true,

) {

    private val emailList: MutableList<String> = ArrayList()

    fun setEmail(email: String): EmailBusinessObject {
        emailList.add(email)
        return this
    }

    /**
     * 发送自定义信息
     *
     * @param content      邮件动态内容
     * @param templateName ftlh名称，例如 mail.ftlh
     */
    fun sendCustomStaticTemplates(content: String?, templateName: String?) {
        sendCustomStaticTemplatesFiles(content, templateName)
    }

    /**
     * 通过默认模板发送自定义Message内容
     */
    fun sendCustomMessage() {
        sendCustomStaticTemplates(message, DEFAULT_MAIL_TEMPLATE)
    }

    /**
     * 通过默认模板发送自定义Message内容和附件
     *
     * @param files 附件
     */
    fun sendCustomMessageFiles(vararg files: File?) {
        sendCustomStaticTemplatesFiles(message, DEFAULT_MAIL_TEMPLATE, *files)
    }


    /**
     * 发送自定义带文件的邮件
     *
     * @param content      邮件动态内容
     * @param templateName ftl路径
     * @param files        附件
     */
    fun sendCustomStaticTemplatesFiles(content: String?, templateName: String?, vararg files: File?) {
        try {
            log.info {
                "send email to: $emailList, templateName: $templateName, content: $content"
            }
            send(this.mailAccount, emailList, title, parseMessages(content, templateName), isHtml, *files)
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }


    /**
     * 发送验证码
     */
    fun sendVerificationCodeMessage(code: String) {
        try {
            send(
                this.mailAccount, this.emailList,
                this.title + "  验证码",
                defaultMailIncludeHtmlTemplates(
                    "mail-vCode.ftlh", code
                ),
                this.isHtml
            )
        } catch (ex: Exception) {
            throw RuntimeException("邮件发送失败", ex)
        }
    }

    fun sendCommentNotification(map: MutableMap<String, String>) {
        try {
            send(
                this.mailAccount,
                this.emailList,
                this.title,
                defaultMailIncludeHtmlTemplates("mail-comment-notification.ftlh", map),
                this.isHtml
            )
        } catch (ex: Exception) {
            throw RuntimeException("邮件发送失败", ex)
        }
    }

    private fun defaultMailIncludeHtmlTemplates(content: String, obj: String): String {
        return parseMessages(content, obj, DEFAULT_MAIL_INCLUDE_HTML_TEMPLATE)
    }

    private fun defaultMailIncludeHtmlTemplates(content: String, map: MutableMap<String, String>): String {
        return parseMessages(content, DEFAULT_MAIL_INCLUDE_HTML_TEMPLATE, map)
    }


    /**
     * @param content      自定义内容
     * @param templateName ftlh路径
     * @return String
     */
    private fun parseMessages(content: String?, templateName: String?): String {
        return FreeMarkerUtils.parseData(mapOf("content" to content), templateName)
    }

    /**
     * ftlh多个参数下
     *
     * @param content 邮件内嵌ftlh路径
     * @return String
     */
    private fun parseMessages(content: String, templateName: String, map: MutableMap<String, String>): String {
        map["content"] = content
        return FreeMarkerUtils.parseData(map, templateName)
    }

    private fun parseMessages(content: String, obj: String, templateName: String): String {
        return FreeMarkerUtils.parseData(mapOf("content" to content, "obj" to obj), templateName)
    }


    /**
     * 发送邮件给多人
     *
     * @param mailAccount 邮件帐户信息
     * @param tos         收件人列表
     * @param subject     标题
     * @param content     正文
     * @param isHtml      是否为HTML格式
     * @param files       附件列表
     */
    private fun send(
        mailAccount: MailAccount?,
        tos: Collection<String>,
        subject: String,
        content: String,
        isHtml: Boolean,
        vararg files: File?
    ) {
        MailUtil.send(mailAccount, tos, null, null, subject, content, null, isHtml, *files)
    }

    companion object {
        // 默认邮件模板
        private const val DEFAULT_MAIL_TEMPLATE = "mail.ftlh"


        private const val DEFAULT_MAIL_INCLUDE_HTML_TEMPLATE = "mail-includeHtml.ftlh"

        private const val DEFAULT_TITLE_PREFIX = "Maa Backend Center"
    }
}
