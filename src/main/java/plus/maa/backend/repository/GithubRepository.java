package plus.maa.backend.repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import plus.maa.backend.repository.entity.GithubContent;

import java.net.URI;
import java.util.List;

/**
 * @author dragove
 * created on 2022/12/23
 */
@FeignClient(value = "githubRepository", url = "https://api.github.com")
public interface GithubRepository {

    /**
     * api doc: <a href="https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28">contents api</a>
     *
     * @param token GitHub api调用token 从 <a href="https://github.com/settings/tokens">tokens</a> 获取
     * @param repo  GitHub 仓库名称（含用户名），例如 MaaAssistantArknights/MaaAssistantArknights
     * @param path  文件路径，例如 src
     */
    @GetMapping(value = "/repos/{repo}/contents/{path}", headers = {
            "Accept: application/vnd.github+json",
            "Authorization: {token}",
            "X-GitHub-Api-Version: 2022-11-28"
    })
    List<GithubContent> getContents(@RequestParam("token") String token,
                                    @RequestParam("repo") String repo,
                                    @RequestParam("path") String path);

    /**
     * 下载
     * @param uri 资源路径
     * @return MAA level json文件
     */
    @GetMapping
    String downloadArknightsTilePos(URI uri);

}
