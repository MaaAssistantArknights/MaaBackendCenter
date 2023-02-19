package plus.maa.backend.config.external;

@lombok.Data
public class CopilotBackup {
    // 是禁用备份功能
    private boolean disabled;
    // 本地备份地址
    private String dir;
    // 远程备份地址
    private String uri;

    // git 用户名
    private String username;
    // git 邮箱
    private String email;
}
