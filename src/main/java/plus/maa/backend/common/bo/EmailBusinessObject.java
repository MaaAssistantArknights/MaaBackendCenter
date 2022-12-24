package plus.maa.backend.common.bo;
import cn.hutool.extra.mail.MailUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author LoMu
 * Date 2022-12-23 23:57
 */
@Slf4j
@Accessors(chain = true)
@Setter
public class EmailBusinessObject {
    private   List<String> emailList;
    //邮件标题
    private   String title;
    //邮件内容
    private  String message;

    //html标签是否被识别使用
    private Boolean isHtml;


    public EmailBusinessObject() {
        isHtml = true;
        emailList  = new ArrayList<>();
    }

    public EmailBusinessObject setEmail(String email) {
            emailList.add(email);
            return this;
    }




    /**
     * 链式编程  没报错就说明发送成功
     * 发送自定义信息
     */
    public void sendCustomMessage(){
        try {
            MailUtil.send(this.emailList
                    ,this.title
                    ,this.message
                    ,this.isHtml
            );
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    /**
     * 链式编程  没报错就说明发送成功
     * 发送自定义信息和附件
     * @param file  附件 io流读文件地址 例:FileUtil.file("d:/aaa.xml")
     */
    public void sendCustomMessageFile(File... file){
        try {
            MailUtil.send(this.emailList
                    ,this.title
                    ,this.message
                    ,this.isHtml
                    ,file);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    /**
     * 发送验证码
     *  等大佬美化html..
     */
    public void sendVerificationCodeMessage(String code){
        try {
            MailUtil.send(this.emailList
                    ,"[Maa Copilot]邮件验证码"
                    ,"<center><h1>Maa Copilot</h1>\n"
                            + "<h5>为了确认您输入的邮箱地址，请输入以下验证码 有效期10分钟。</h5>\n"
                            + "<h3>\n" + code + "</h3>\n"
                            + "※此邮件为自动发送，请不要回复此邮件。<br>\n"
                            + "※如果您没有进行相关操作而受到了此邮件，<br>\n"
                            + "可能是他人输入了错误的邮箱地址，请删除此邮件。<br>\n"
                            + "<a href='https://maa.plus/' target=\"_blank\">"
                            + "[MaaAssistantArknights]"
                            + "</a></center>"
                    ,this.isHtml
                    );
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }




    /**
     * 测试
     */
    public void TestEmail(){
        try {
            MailUtil.send(this.emailList
                    ,"Maa Backend Center"
                    ,"This is a Test email"
                    ,this.isHtml
                    );
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }


}
