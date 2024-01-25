package plus.maa.backend.config.external;

import lombok.Data;

/**
 * @author LoMu
 * Date  2023-03-04 14:49
 */
@Data
public class Mail {
    private String host;
    private Integer port;
    private String from;
    private String user;
    private String pass;
    private Boolean starttls;
    private Boolean ssl;
    private Boolean notification;
}
