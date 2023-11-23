package plus.maa.backend.repository;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import plus.maa.backend.repository.entity.github.GithubCommit;
import plus.maa.backend.repository.entity.github.GithubContent;
import plus.maa.backend.repository.entity.github.GithubTrees;

import java.util.List;

/**
 * @author dragove
 * created on 2022/12/23
 */
public interface GithubRepository {

    /**
     * api doc: <a href="https://docs.github.com/en/rest/git/trees?apiVersion=2022-11-28#get-a-tree">git trees api</a>
     */
    @GetExchange(value = "/repos/MaaAssistantArknights/MaaAssistantArknights/git/trees/{sha}")
    GithubTrees getTrees(@RequestHeader("Authorization") String token, @PathVariable("sha") String sha);

    @GetExchange(value = "/repos/MaaAssistantArknights/MaaAssistantArknights/commits")
    List<GithubCommit> getCommits(@RequestHeader("Authorization") String token);

    @GetExchange(value = "/repos/MaaAssistantArknights/MaaAssistantArknights/contents/{path}")
    List<GithubContent> getContents(@RequestHeader("Authorization") String token, @PathVariable("path") String path);

}
