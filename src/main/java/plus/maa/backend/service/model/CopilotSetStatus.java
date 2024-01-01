package plus.maa.backend.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author dragove
 * create on 2024-01-01
 */
@Getter
@AllArgsConstructor
public enum CopilotSetStatus {

    PUBLIC(1),
    PRIVATE(2),
    ;

    private final int code;

    /**
     * 枚举数量较少，暂时这么写
     * @param code 数据库中的数值
     * @return 枚举对象
     */
    public static CopilotSetStatus get(int code) {
        return code == 1 ? PUBLIC : PRIVATE;
    }

}
