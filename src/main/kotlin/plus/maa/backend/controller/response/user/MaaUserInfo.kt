package plus.maa.backend.controller.response.user

import plus.maa.backend.repository.entity.MaaUser

/**
 * 用户可对外公开的信息
 *
 * @author AnselYuki
 */
data class MaaUserInfo(
    val id: String,
    val userName: String,
    val activated: Boolean = false,
    var followingCount: Int = 0,
    var fansCount: Int = 0,
) {
    constructor(user: MaaUser) : this(user.userId!!, user.userName, user.status == 1)
}
