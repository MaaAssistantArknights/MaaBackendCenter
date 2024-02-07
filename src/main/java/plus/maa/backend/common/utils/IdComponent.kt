package plus.maa.backend.common.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import plus.maa.backend.repository.entity.CollectionMeta
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val log = KotlinLogging.logger {  }

@Component
class IdComponent(
    private val mongoTemplate: MongoTemplate
) {
    private val currentIdMap: MutableMap<String, AtomicLong> = ConcurrentHashMap()

    /**
     * 获取id数据
     * @param meta 集合元数据
     * @return 新的id
     */
    fun <T> getId(meta: CollectionMeta<T>): Long {
        val cls = meta.entityClass
        val collectionName = mongoTemplate.getCollectionName(cls)
        var v = currentIdMap[collectionName]
        if (v == null) {
            synchronized(cls) {
                v = currentIdMap[collectionName]
                if (v == null) {
                    v = AtomicLong(getMax(cls, meta.idGetter, meta.incIdField))
                    log.info { "初始化获取 collection: $collectionName 的最大 id，id: ${v!!.get()}" }
                    currentIdMap[collectionName] = v!!
                }
            }
        }
        return v!!.incrementAndGet()
    }

    private fun <T> getMax(entityClass: Class<T>, idGetter: (T)->Long, fieldName: String) =
        mongoTemplate.findOne(
            Query().with(Sort.by(fieldName).descending()).limit(1),
            entityClass
        )
            ?.let(idGetter)
            ?: 20000L
}
