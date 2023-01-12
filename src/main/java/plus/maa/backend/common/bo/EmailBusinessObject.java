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


    private List<String> emailList = new ArrayList<>();
    //邮件标题
    private String title = "Maa Backend Center";

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
        this.title = this.title + "  " + title;
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
     * 发送Message内容
     */
    public void sendCustomMessage() {
        try {
            MailUtil.send(this.emailList
                    , this.title
                    , this.message
                    , this.isHtml
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
     * @param file    附件
     */
    public void sendCustomStaticTemplatesFile(String content, String path, File... file) {
        try {
            MailUtil.send(this.emailList
                    , this.title
                    , this.parseMessages(content, path)
                    , this.isHtml
                    , file);
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
                    , this.title
                   /* , """
                            <center>
                            <h1>Maa Backend Center</h1>
                            <h5>为了确认您输入的邮箱地址，请输入以下验证码 有效期10分钟。</h5>
                            <h3> %s </h3>
                            ※此邮件为自动发送，请不要回复此邮件。<br>
                            ※如果您没有进行相关操作而受到了此邮件，<br>
                            可能是他人输入了错误的邮箱地址，请删除此邮件。<br>
                            <a href='https://maa.plus/' target="_blank">
                            [MaaAssistantArknights]
                            </a> </center>
                            """.formatted(code)*/
                    , parseMessages(code, "static/templates/mail.ftl")
                    , this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * 测试
     */
    public void TestEmail() {
        try {
            MailUtil.send(this.emailList,
                    this.title,
                    parseMessages("6666", "static/templates/mail.ftl"),
                    this.isHtml
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
        InputStream inputStream = null;
        BufferedReader fileReader = null;
        StringBuilder buffer = new StringBuilder();
        String line;
        try {
            inputStream = resource.getInputStream();
            fileReader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = fileReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            log.error("邮件解析失败{}", e.toString());
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //替换html模板中的参数
        return MessageFormat.format(buffer.toString(), content);
    }


}
