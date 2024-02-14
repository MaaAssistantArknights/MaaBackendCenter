package plus.maa.backend.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author LoMu
 * Date  2023-01-28 11:37
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingCache {
    private Set<Long> copilotIds;
}
