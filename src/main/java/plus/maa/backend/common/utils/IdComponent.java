package plus.maa.backend.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.CollectionMeta;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdComponent {
    private final MongoTemplate mongoTemplate;
    private final Map<String, AtomicLong> CURRENT_ID_MAP = new ConcurrentHashMap<>();

    /**
     * 获取id数据
     * @param meta 集合元数据
     * @return 新的id
     */
    public <T> Long getId(CollectionMeta<T> meta) {
        Class<T> cls = meta.entityClass();
        String collectionName = mongoTemplate.getCollectionName(cls);
        AtomicLong v = CURRENT_ID_MAP.get(collectionName);
        if (v == null) {
            synchronized (cls) {
                v = CURRENT_ID_MAP.get(collectionName);
                if (v == null) {
                    v = new AtomicLong(getMax(cls, meta.idGetter(), meta.incIdField()));
                    log.info("初始化获取 collection: {} 的最大 id，id: {}", collectionName, v.get());
                    CURRENT_ID_MAP.put(collectionName, v);
                }
            }
        }
        return v.incrementAndGet();
    }

    private <T> Long getMax(Class<T> entityClass, Function<T, Long> idGetter, String fieldName) {
        return Optional.ofNullable(mongoTemplate.findOne(
                        new Query().with(Sort.by(fieldName).descending()).limit(1),
                        entityClass))
                .map(idGetter)
                .orElse(20000L);
    }
}
