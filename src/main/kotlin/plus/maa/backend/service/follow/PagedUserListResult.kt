package plus.maa.backend.service.follow

import plus.maa.backend.repository.entity.MaaUser

data class PagedUserListResult(val total: Long, val paged: List<MaaUser>)
