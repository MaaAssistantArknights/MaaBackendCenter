package plus.maa.backend.common.bo;


import cn.hutool.extra.mail.MailUtil;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import plus.maa.backend.common.utils.FreeMarkerUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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

    private static final String DEFAULT_TITLE_PREFIX = "Maa Backend Center";

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
        this.title = DEFAULT_TITLE_PREFIX + "  " + title;
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
            MailUtil.send(emailList, title, parseMessages(content, templateName), isHtml, files);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * 发送验证码
     */
    public void sendVerificationCodeMessage(String code) {

        try {
            MailUtil.send(this.emailList
                    , this.title + "  验证码"
                    , defaultMailTemplates(
                            MessageFormat.format(
                                    """
                                                <h1 style=" font-size: 28px; margin: 0; padding: 0; color: #5c5c5c">
                                                     Maa Backend Center
                                                </h1>
                                                <h2 style="padding-bottom: 3%; color: #5c5c5c; margin: 1% 0 0 0">
                                                     验证你的账户
                                                </h2>
                                                <h1 style=" color: #333333; font-size: 28px; font-weight: 400; line-height: 1.4; margin: 0; padding-bottom: 4%">
                                                     {0}
                                                </h1>
                                                <p style="font-size: 10px">为了确认您输入的邮箱地址，请输入以上验证码 有效期10分钟</p>
                                            """
                                    , code)
                    )
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException("邮件发送失败", ex);
        }
    }


    public void sendActivateUrlMessage(String url) {

        try {
            MailUtil.send(this.emailList
                    , this.title + "  账户激活"
                    , defaultMailTemplates(
                            MessageFormat.format(
                                    """
                                                 <h1 style=" font-size: 28px; margin: 0; padding: 0; color: #5c5c5c">
                                                    Maa Backend Center
                                                 </h1>
                                                 <h1 style=" color: #333333; font-size: 28px; font-weight: 400; line-height: 1.4; margin: 0; padding: 4% 0">
                                                     <a href="{0}">
                                                        <button style="font-size: 30px; color:#ffffff; border:0;  background-color: transparent; opacity: 100%;border-radius: 5px;padding: 1% 4%;background: linear-gradient(to bottom, rgba(255,255,255,0.15) 0%, rgba(0,0,0,0.15) 100%), radial-gradient(at top center, rgba(255,255,255,0.40) 0%, rgba(0,0,0,0.40) 120%) #989898;background-blend-mode: multiply,multiply;">
                                                            验证你的账户
                                                        </button>
                                                     </a>
                                                 </h1>
                                                 <p style="font-size: 10px">为了确认您输入的邮箱地址，请点击以上链接 有效期10分钟</p>
                                            """
                                    , url)
                    )
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException("邮件发送失败", ex);
        }
    }


    /**
     * 默认邮件模板
     *
     * @param content 自定义html内容
     * @return 封装邮件模板内容
     */
    private String defaultMailTemplates(String content) {
        return parseMessages(content, DEFAULT_MAIL_TEMPLATE);
    }


    /**
     * 将ftl文件转换为String对象
     *
     * @param content      邮件动态内容
     * @param templateName ftlh路径
     * @return String
     */
    private String parseMessages(String content, String templateName) {
        return FreeMarkerUtils.parseData(Collections.singletonMap("content", content), templateName);
    }

}
