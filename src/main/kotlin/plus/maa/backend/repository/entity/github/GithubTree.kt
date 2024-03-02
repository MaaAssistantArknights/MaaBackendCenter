package plus.maa.backend.repository.entity.github

/**
 * @author john180
 */
data class GithubTree (
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val url: String?
)
