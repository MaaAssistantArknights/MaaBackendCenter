package plus.maa.backend.common.model;

import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * @author dragove
 * create on 2024-01-01
 */
public interface CopilotSetType {

    List<Long> getCopilotIds();

    default List<Long> getDistinctIdsAndCheck() {
        List<Long> copilotIds = getCopilotIds();
        if (copilotIds == null) {
            return Collections.emptyList();
        }
        if (copilotIds.isEmpty() || copilotIds.size() == 1) {
            return getCopilotIds();
        }
        copilotIds = copilotIds.stream().distinct().toList();
        Assert.state(copilotIds.size() <= 1000, "作业集总作业数量不能超过1000条");
        return copilotIds;
    }

}
