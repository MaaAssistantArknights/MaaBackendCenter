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
     * 发送邮件
     */
    public boolean sendMessage(MaaEmail maaEmail){
      try {
          MailUtil.send(maaEmail.getToEmail(),maaEmail.getTitle(),maaEmail.getMessage(),true);
          return true;
      }catch (Exception ex){
         throw new RuntimeException(ex);
      }
    }

}
