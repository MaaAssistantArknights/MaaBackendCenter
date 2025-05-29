package plus.maa.backend.common.controller

import org.springframework.data.domain.Page

fun <T> Page<T>.toDto() = PagedDTO(hasNext(), pageable.pageNumber + 1, totalElements, content)
