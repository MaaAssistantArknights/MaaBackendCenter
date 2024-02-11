package plus.maa.backend.service.model

/**
 * @author dragove
 * create on 2024-01-01
 */
enum class CopilotSetStatus {
    /**
     * 私有，仅查看自己的作业集的时候展示，其他列表页面不展示，但是通过详情接口可查询（无权限控制）
     */
    PRIVATE,

    /**
     * 公开，可以被搜索
     */
    PUBLIC,
}
