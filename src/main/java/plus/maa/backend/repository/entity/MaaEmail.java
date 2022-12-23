package plus.maa.backend.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.File;

/**
 * @author LoMu
 * Date  2022-12-24 0:25
 */

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor

//这是一条临时的邮件实体类 没有配置批量发送邮件 仅供测试 白天改
public class MaaEmail {
    //接收邮箱
    private String toEmail;
    //邮件标题
    private String Title;
    //邮件内容
    private String Message;
    //附件
    private File file;
    //是否为html
    private Boolean isHtml = false;

}
