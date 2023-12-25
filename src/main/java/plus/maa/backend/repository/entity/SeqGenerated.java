package plus.maa.backend.repository.entity;

import org.springframework.data.annotation.Transient;

/**
 * 可用于生成id的数据类型
 */
public interface SeqGenerated {
    /**
     * 用于生成该类型id的sequenceName
     * 方法名这么长是为了防止重名
     */
    @Transient
    Long getGeneratedId();

    @Transient
    String getIdFieldName();
}
