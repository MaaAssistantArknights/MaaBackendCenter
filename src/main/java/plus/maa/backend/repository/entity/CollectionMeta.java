package plus.maa.backend.repository.entity;

import java.io.Serializable;
import java.util.function.Function;

/**
 * mongodb 集合元数据
 *
 * @param <T> 集合对应实体数据类型
 * @author dragove
 * created on 2023-12-27
 */
public record CollectionMeta<T>(Function<T, Long> idGetter, String incIdField, Class<T> entityClass)
        implements Serializable {

}
