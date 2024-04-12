package plus.maa.backend.repository.entity

import java.io.Serializable

/**
 * mongodb 集合元数据
 *
 * @param <T> 集合对应实体数据类型
 * @author dragove
 * created on 2023-12-27
 */
data class CollectionMeta<T>(
    val idGetter: (T) -> Long,
    val incIdField: String,
    val entityClass: Class<T>,
) : Serializable
