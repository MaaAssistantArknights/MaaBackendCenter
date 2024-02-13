package plus.maa.backend.repository.entity.github;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author john180
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubTrees {
    private String sha;
    private String url;
    private List<GithubTree> tree;
}
