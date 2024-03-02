package plus.maa.backend.controller.request.copilot

import jakarta.validation.constraints.Max

/**
 * @author LoMu
 * Date  2022-12-26 2:48
 */
data class CopilotQueriesRequest(
    val page: Int = 0,
    val limit: @Max(value = 50, message = "单页大小不得超过50") Int = 10,
    var levelKeyword: String? = null,
    val level_keyword: String? = null,
    val operator: String? = null,
    val content: String? = null,
    val document: String? = null,
    var uploaderId: String? = null,
    val uploader_id: String? = null,
    val desc: Boolean = true,
    var orderBy: String? = null,
    val order_by: String? = null,
    val language: String? = null,
    var copilotIds: List<Long>? = null,
    val copilot_ids: List<Long>? = null
) {

//    /*
//     * 这里为了正确接收前端的下划线风格，手动写了三个 setter 用于起别名
//     * 因为 Get 请求传入的参数不是 JSON，所以没办法使用 Jackson 的注解直接实现别名
//     * 添加 @JsonAlias 和 @JsonIgnore 注解只是为了保障 Swagger 的文档正确显示
//     * （吐槽一下，同样是Get请求，怎么CommentsQueries是驼峰命名，到了CopilotQueries就成了下划线命名）
//     */
//    @JsonIgnore
//    @Suppress("unused")
//    fun setLevel_keyword(levelKeyword: String?) {
//        this.levelKeyword = levelKeyword
//    }
//
//    @JsonIgnore
//    @Suppress("unused")
//    fun setUploader_id(uploaderId: String?) {
//        this.uploaderId = uploaderId
//    }
//
//    @JsonIgnore
//    @Suppress("unused")
//    fun setCopilot_ids(copilotIds: List<Long>?) {
//        this.copilotIds = copilotIds
//    }
}
