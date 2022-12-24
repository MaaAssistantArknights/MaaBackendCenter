package plus.maa.backend.common.utils;
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
public class EmailUtils  {
    private   List<String> emailList;
    //邮件标题
    private   String title;
    //邮件内容
    private  String message;

    //html标签是否被识别使用
    private Boolean isHtml;

    public EmailUtils() {
        isHtml = true;
        emailList  = new ArrayList<>();
    }

    public  EmailUtils setEmail(String email) {
            emailList.add(email);
            return this;
    }


    /**
     * 链式编程  没报错就说明发送成功
     * 发送自定义信息
     */
    public boolean sendCustomMessageList(){
        try {
            MailUtil.send(this.emailList
                    ,this.title
                    ,this.message
                    ,this.isHtml
            );
            return true;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    /**
     * 链式编程  没报错就说明发送成功
     * 发送自定义信息和附件
     * @param file  附件 io流读文件地址 例:FileUtil.file("d:/aaa.xml")
     */
    public void sendCustomMessageFileList(File... file){
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
