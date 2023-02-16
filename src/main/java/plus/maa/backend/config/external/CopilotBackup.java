package plus.maa.backend.config.external;

@lombok.Data
public class CopilotBackup {
    private boolean disabled;
    private String dir;
    private String uri;

    private String userName;
    private String email;
}
