package plus.maa.backend.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;

/**
 * @author LoMu
 * Date  2022-12-24 0:25
 */

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class MaaEmail {
    /**
     * @apiNote
     */
    private List<String> toEmail;
    //邮件标题
    private String Title;
    //邮件内容
    private String Message;
    //附件 读文件地址即可  例:FileUtil.file("d:/aaa.xml")
    private File file;
    //html标签是否被识别使用
    private Boolean isHtml = true;

}
