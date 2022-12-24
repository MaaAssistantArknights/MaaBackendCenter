package plus.maa.backend.repository.entity.github;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author john180
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubCommit {
    private String sha;
}
