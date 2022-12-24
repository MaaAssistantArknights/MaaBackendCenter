package plus.maa.backend.repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import plus.maa.backend.repository.entity.github.GithubCommit;
import plus.maa.backend.repository.entity.github.GithubTrees;

import java.util.List;

/**
 * @author dragove
 * created on 2022/12/23
 */
@FeignClient(value = "githubRepository", url = "https://api.github.com")
public interface GithubRepository {

    /**
     * api doc: <a href="https://docs.github.com/en/rest/git/trees?apiVersion=2022-11-28#get-a-tree">git trees api</a>
     */
    @GetMapping(value = "/repos/{repo}/git/trees/{sha}",
            headers = {
                    "Accept: application/vnd.github+json",
                    "Authorization: {token}",
                    "X-GitHub-Api-Version: 2022-11-28"
            })
    GithubTrees getTrees(@RequestParam("token") String token,
                         @PathVariable("repo") String repo,
                         @PathVariable("sha") String sha);

    @GetMapping(value = "/repos/{repo}/commits",
            headers = {
                    "Accept: application/vnd.github+json",
                    "Authorization: {token}",
                    "X-GitHub-Api-Version: 2022-11-28"
            })
    List<GithubCommit> getCommits(@RequestParam("token") String token,
                                  @PathVariable("repo") String repo,
                                  @RequestParam("path") String path,
                                  @RequestParam("per_page") int prePage);
}
