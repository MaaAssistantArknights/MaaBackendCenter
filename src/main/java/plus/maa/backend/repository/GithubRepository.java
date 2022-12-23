package plus.maa.backend.repository;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import plus.maa.backend.repository.entity.ArknightsTilePos;
import plus.maa.backend.repository.entity.GithubContent;

import java.net.URI;
import java.util.List;

/**
 * @author dragove
 * created on 2022/12/23
 */
public interface GithubRepository {

    /**
     * api doc: <a href="https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28">contents api</a>
     *
     * @param token GitHub api调用token 从 <a href="https://github.com/settings/tokens">tokens</a> 获取
     * @param repo  GitHub 仓库名称（含用户名），例如 MaaAssistantArknights/MaaAssistantArknights
     * @param path  文件路径，例如 src
     */
    @Headers({
            "Accept: application/vnd.github+json",
            "Authorization: {token}",
            "X-GitHub-Api-Version: 2022-11-28"
    })
    @RequestLine("GET /repos/{repo}/contents/{path}")
    List<GithubContent> getContents(@Param("token") String token, @Param("repo") String repo,
                                    @Param("path") String path);

    /**
     * 下载
     * @param uri 资源路径
     * @return MAA level json文件
     */
    @RequestLine("GET")
    ArknightsTilePos downloadArknightsTilePos(URI uri);

}
