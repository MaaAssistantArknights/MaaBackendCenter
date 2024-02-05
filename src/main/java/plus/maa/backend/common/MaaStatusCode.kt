package plus.maa.backend.common

/**
 * @author AnselYuki
 */
enum class MaaStatusCode(val code:Int,val message:String) {
    /**
     * MAA自定义状态码
     */
    MAA_USER_NOT_FOUND(10001, "找不到用户"),
    MAA_ACTIVE_ERROR(10002, "激活失败"),
    MAA_USER_NOT_ENABLED(10003, "用户未启用"),
    MAA_USER_EXISTS(10004, "用户已存在"),
    MAA_REGISTRATION_CODE_NOT_FOUND(10011, "注册验证码错误"),

}
