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
public class GithubTree {
    private String path;
    private String mode;
    private String type;
    private String sha;
    private String url;
}
