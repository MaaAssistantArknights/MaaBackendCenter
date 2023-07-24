package plus.maa.backend.common.bo;


import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import plus.maa.backend.common.utils.FreeMarkerUtils;

import java.io.File;
import java.util.*;


/**
 * @author LoMu
 * Date 2022-12-23 23:57
 */
@Slf4j
@Accessors(chain = true)
@Setter
@NoArgsConstructor
public class EmailBusinessObject {

    // 默认邮件模板
    private static final String DEFAULT_MAIL_TEMPLATE = "mail.ftlh";


    private static final String DEFAULT_MAIL_INCLUDE_HTML_TEMPLATE = "mail-includeHtml.ftlh";

    private static final String DEFAULT_TITLE_PREFIX = "Maa Backend Center";

    //发件人信息
    private MailAccount mailAccount;

    private List<String> emailList = new ArrayList<>();

    // 自定义标题
    private String title = DEFAULT_TITLE_PREFIX;

    // 邮件内容
    private String message;

    // html标签是否被识别使用
    private Boolean isHtml = true;


    /**
     * 静态创建工厂
     *
     * @return EmailBusinessObject
     */
    public static EmailBusinessObject builder() {
        return new EmailBusinessObject();
    }


    public EmailBusinessObject setEmail(String email) {
        emailList.add(email);
        return this;
    }

    /**
     * 设置邮件标题 默认为 Maa Backend Center
     *
     * @param title 标题
     */
    public EmailBusinessObject setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * 发送自定义信息
     *
     * @param content      邮件动态内容
     * @param templateName ftlh名称，例如 mail.ftlh
     */
    public void sendCustomStaticTemplates(String content, String templateName) {
        sendCustomStaticTemplatesFiles(content, templateName, (File[]) null);
    }

    /**
     * 通过默认模板发送自定义Message内容
     */
    public void sendCustomMessage() {
        sendCustomStaticTemplates(message, DEFAULT_MAIL_TEMPLATE);
    }

    /**
     * 通过默认模板发送自定义Message内容和附件
     *
     * @param files 附件
     */
    public void sendCustomMessageFiles(File... files) {
        sendCustomStaticTemplatesFiles(message, DEFAULT_MAIL_TEMPLATE, files);
    }


    /**
     * 发送自定义带文件的邮件
     *
     * @param content      邮件动态内容
     * @param templateName ftl路径
     * @param files        附件
     */
    public void sendCustomStaticTemplatesFiles(String content, String templateName, File... files) {
        try {
            log.info("send email to: {}, templateName: {}, content: {}", emailList, templateName, content);
            send(this.mailAccount, emailList, title, parseMessages(content, templateName), isHtml, files);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * 发送验证码
     */
    public void sendVerificationCodeMessage(String code) {

        try {
            send(this.mailAccount, this.emailList
                    , this.title + "  验证码"
                    , defaultMailIncludeHtmlTemplates(
                            "mail-vCode.ftlh", code
                    )
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException("邮件发送失败", ex);
        }
    }


    public void sendActivateUrlMessage(String url) {

        try {
            send(this.mailAccount, this.emailList
                    , this.title + "  账户激活"
                    , defaultMailIncludeHtmlTemplates(
                            "mail-activateUrl.ftlh", url
                    )
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException("邮件发送失败", ex);
        }
    }

    public void sendCommentNotification(Map<String, String> map) {
        try {
            send(this.mailAccount,
                    this.emailList,
                    this.title,
                    defaultMailIncludeHtmlTemplates("mail-comment-notification.ftlh", map),
                    this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException("邮件发送失败", ex);
        }
    }

    private String defaultMailIncludeHtmlTemplates(String content, String obj) {
        return parseMessages(content, obj, DEFAULT_MAIL_INCLUDE_HTML_TEMPLATE);
    }

    private String defaultMailIncludeHtmlTemplates(String content, Map<String, String> map) {
        return parseMessages(content, DEFAULT_MAIL_INCLUDE_HTML_TEMPLATE, map);
    }


    /**
     * @param content      自定义内容
     * @param templateName ftlh路径
     * @return String
     */
    private String parseMessages(String content, String templateName) {
        return FreeMarkerUtils.parseData(Collections.singletonMap("content", content), templateName);
    }

    /**
     * ftlh多个参数下
     *
     * @param content 邮件内嵌ftlh路径
     * @return String
     */
    private String parseMessages(String content, String templateName, Map<String, String> map) {
        map.put("content", content);
        return FreeMarkerUtils.parseData(map, templateName);
    }

    private String parseMessages(String content, String obj, String templateName) {
        return FreeMarkerUtils.parseData(Map.of("content", content, "obj", obj), templateName);
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
    private void send(MailAccount mailAccount, Collection<String> tos, String subject, String content, boolean isHtml, File... files) {
        MailUtil.send(mailAccount, tos, null, null, subject, content, null, isHtml, files);
    }
}
