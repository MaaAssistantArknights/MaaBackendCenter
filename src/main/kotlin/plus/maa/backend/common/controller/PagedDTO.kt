package plus.maa.backend.common.controller

data class PagedDTO<T>(
    val hasNext: Boolean,
    val page: Int,
    val total: Long,
    val data: List<T>,
)
