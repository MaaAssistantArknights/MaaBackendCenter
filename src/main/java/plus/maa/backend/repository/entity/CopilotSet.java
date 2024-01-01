package plus.maa.backend.repository.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import plus.maa.backend.service.model.CopilotSetStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业集数据
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@Document("maa_copilot_set")
public class CopilotSet implements Serializable {

    @Transient
    public static final CollectionMeta<CopilotSet> META = new CollectionMeta<>(CopilotSet::getId,
            "id", CopilotSet.class);

    /**
     * 作业集id
     */
    @Id
    private long id;

    /**
     * 作业集名称
     */
    private String name;

    /**
     * 额外描述
     */
    private String description;

    /**
     * 作业id列表
     * 使用 list 保证有序
     * 作业添加时应当保证唯一
     */
    private List<Long> copilotIds;

    /**
     * 上传者id
     */
    private String creatorId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 作业状态
     * {@link plus.maa.backend.service.model.CopilotSetStatus}
     */
    private CopilotSetStatus status;

}
