package plus.maa.backend.controller.response.user

import plus.maa.backend.repository.entity.MaaUser

/**
 * @author AnselYuki
 */
data class MaaUserInfo(
    val id: String,
    val userName: String,
    val activated: Boolean = false
) {
    constructor(user: MaaUser) : this(user.userId, user.userName, user.status == 1)
}
