package plus.maa.backend.common.bo;


import cn.hutool.extra.mail.MailUtil;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;


import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
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

    private final String DEFAULT_TITLE_PREFIX = "Maa Backend Center";

    private List<String> emailList = new ArrayList<>();

    //自定义标题
    private String title = DEFAULT_TITLE_PREFIX;

    //邮件内容
    private String message;

    //html标签是否被识别使用
    private Boolean isHtml = true;


    /**
     * 静态创建工厂
     *
     * @return EmailBusinessObject
     */
    public static EmailBusinessObject Builder() {
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
    public void setTitle(String title) {
        this.title = this.DEFAULT_TITLE_PREFIX + "  " + title;
    }

    /**
     * 发送自定义信息
     *
     * @param content 邮件动态内容
     * @param path    ftl路径
     */
    public void sendCustomStaticTemplates(String content, String path) {
        try {
            MailUtil.send(this.emailList
                    , this.title
                    , this.parseMessages(content, path)
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 通过默认模板发送自定义Message内容
     */
    public void sendCustomMessage() {
        try {
            MailUtil.send(this.emailList
                    , this.title
                    , defaultMailTemplates(this.message)
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 通过默认模板发送自定义Message内容和附件
     *
     * @param files 附件
     */
    public void sendCustomMessageFiles(File... files) {
        try {
            MailUtil.send(this.emailList
                    , this.title
                    , defaultMailTemplates(this.message)
                    , this.isHtml
                    , files
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * 发送自定义带文件的邮件
     *
     * @param content 邮件动态内容
     * @param path    ftl路径
     * @param files   附件
     */
    public void sendCustomStaticTemplatesFiles(String content, String path, File... files) {
        try {
            MailUtil.send(this.emailList
                    , this.title
                    , this.parseMessages(content, path)
                    , this.isHtml
                    , files);
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
            throw new RuntimeException(ex);
        }
    }


    /**
     * 默认邮件模板
     *
     * @param content 自定义html内容
     * @return 封装邮件模板内容
     */
    private String defaultMailTemplates(String content) {
        return parseMessages(content, "static/templates/mail.ftl");
    }


    /**
     * 将ftl文件转换为String对象
     *
     * @param content 邮件动态内容
     * @param path    ftl路径
     * @return String
     */

    private String parseMessages(String content, String path) {

        Resource resource = new ClassPathResource(path);
        StringBuilder buffer = new StringBuilder();
        String line;
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = fileReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            log.error("邮件解析失败", e);
        }

        return MessageFormat.format(buffer.toString(), content);
    }


}
