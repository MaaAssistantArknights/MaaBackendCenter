package plus.maa.backend.repository.entity.github;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author dragove
 * created on 2022/12/23
 */
@Data
public class GithubContent {

    // 文件名
    private String name;
    // 路径
    private String path;
    private String sha;
    // 文件大小(Byte)
    private Long size;
    // 路径类型 file-文件 dir-目录
    private String type;
    // 下载地址
    private String downloadUrl;
    // 访问地址
    private String htmlUrl;
    // 对应commit地址
    private String gitUrl;

    /**
     * 仿照File类，判断是否目录类型
     *
     * @return 如果是目录类型，则返回 true，文件类型则返回 false
     */
    public boolean isDir() {
        return Objects.equals(type, "dir");
    }

    public String getFileExtension() {
        return name == null ?
                StringUtils.EMPTY :
                (name.contains(".") ?
                        name.substring(name.lastIndexOf(".") + 1) :
                        StringUtils.EMPTY
                );
    }

}
