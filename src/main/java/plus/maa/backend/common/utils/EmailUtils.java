package plus.maa.backend.common.utils;

import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.MaaEmail;

import java.text.MessageFormat;


/**
 * @author LoMu
 * Date 2022-12-23 23:57
 */
@Slf4j
@Component
public class EmailUtils  {


    /**
     * 发送信息
     * @param maaEmail 邮件类(邮件接收者,邮件信息,附件)
     * @return boolean
     */
    public boolean sendMessage(MaaEmail maaEmail){
      try {
          MailUtil.send(maaEmail.getToEmail()
                  ,maaEmail.getTitle()
                  ,maaEmail.getMessage()
                  ,maaEmail.getIsHtml());
          return true;
      }catch (Exception ex){
         throw new RuntimeException(ex);
      }
    }


    /**
     * 发送信息和附件
     * @param maaEmail 邮件类(邮件接收者,邮件信息,附件)
     * @return  boolean
     */
    public boolean sendMessageFile(MaaEmail maaEmail){
        try {
            MailUtil.send(maaEmail.getToEmail()
                    ,maaEmail.getTitle()
                    ,maaEmail.getMessage()
                    ,maaEmail.getIsHtml()
                    ,maaEmail.getFile());
            return true;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

}
