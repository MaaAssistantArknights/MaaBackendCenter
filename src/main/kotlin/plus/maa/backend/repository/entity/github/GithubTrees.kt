package plus.maa.backend.repository.entity.github

/**
 * @author john180
 */
class GithubTrees(
    val sha: String,
    val url: String,
    val tree: List<GithubTree>,
)
