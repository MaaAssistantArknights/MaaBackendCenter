package plus.maa.backend.repository.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

/**
 * 作业集数据
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@Document("maa_copilot_set")
public class CopilotSet implements Serializable {

    public static final CollectionMeta<CopilotSet> META = new CollectionMeta<>(CopilotSet::getId,
            "id", CopilotSet.class);

    /**
     * 作业集id
     */
    @Id
    private Long id;

    /**
     * 作业id列表
     * 使用 list 保证有序
     * 作业添加时应当保证唯一
     */
    private List<Long> copilotIds;

    /**
     * 上传者id
     */
    private String uploaderId;

}
