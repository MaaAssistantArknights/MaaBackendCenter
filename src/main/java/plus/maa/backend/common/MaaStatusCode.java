package plus.maa.backend.common;

import lombok.AllArgsConstructor;

/**
 * @author AnselYuki
 */
@AllArgsConstructor
public enum MaaStatusCode {
    /**
     * MAA自定义状态码
     */
    MAA_USER_NOT_FOUND(10002, "找不到用户"),
    MAA_ACTIVE_ERROR(10003, "激活失败");
    public final int code;
    public final String message;
}
