package plus.maa.backend.repository

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import plus.maa.backend.repository.entity.github.GithubCommit
import plus.maa.backend.repository.entity.github.GithubContent
import plus.maa.backend.repository.entity.github.GithubTrees

/**
 * @author dragove
 * created on 2022/12/23
 */
interface GithubRepository {
    /**
     * api doc: [git trees api](https://docs.github.com/en/rest/git/trees?apiVersion=2022-11-28#get-a-tree)
     */
    @GetExchange(value = "/repos/MaaAssistantArknights/MaaAssistantArknights/git/trees/{sha}")
    fun getTrees(@RequestHeader("Authorization") token: String, @PathVariable("sha") sha: String): GithubTrees

    @GetExchange(value = "/repos/MaaAssistantArknights/MaaAssistantArknights/commits")
    fun getCommits(@RequestHeader("Authorization") token: String): List<GithubCommit>

    @GetExchange(value = "/repos/MaaAssistantArknights/MaaAssistantArknights/contents/{path}")
    fun getContents(
        @RequestHeader("Authorization") token: String,
        @PathVariable("path") path: String
    ): List<GithubContent>
}
