package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.service.UserFlowService

@Tag(name = "UserFlow", description = "用户关注相关接口")
@RestController
@RequestMapping("/user/flow")
class UserFlowController(
    private val userFlowService: UserFlowService,
    private val helper: AuthenticationHelper
) {
    @Operation(summary = "关注用户")
    @ApiResponse(description = "关注结果")
    @RequireJwt
    @PostMapping("/follow/{targetUserId}")
    fun follow(@PathVariable targetUserId: String): MaaResult<Unit> {
        userFlowService.follow(helper.requireUserId(), targetUserId)
        return success()
    }

    @Operation(summary = "取消关注")
    @ApiResponse(description = "取消关注结果")
    @RequireJwt
    @PostMapping("/unfollow/{targetUserId}")
    fun unfollow(@PathVariable targetUserId: String): MaaResult<Unit> {
        userFlowService.unfollow(helper.requireUserId(), targetUserId)
        return success()
    }

    @Operation(summary = "获取关注列表")
    @ApiResponse(description = "关注列表")
    @RequireJwt
    @GetMapping("/following")
    fun getFollowingList(pageable: Pageable): MaaResult<Page<MaaUserInfo>> {
        return success(userFlowService.getFollowingList(helper.requireUserId(), pageable))
    }

    @Operation(summary = "获取粉丝列表")
    @ApiResponse(description = "粉丝列表")
    @RequireJwt
    @GetMapping("/followers")
    fun getFollowerList(pageable: Pageable): MaaResult<Page<MaaUserInfo>> {
        return success(userFlowService.getFollowerList(helper.requireUserId(), pageable))
    }

    @Operation(summary = "获取关注数量")
    @ApiResponse(description = "关注数量")
    @RequireJwt
    @GetMapping("/following/count")
    fun getFollowingCount(): MaaResult<Long> {
        return success(userFlowService.getFollowingCount(helper.requireUserId()))
    }

    @Operation(summary = "获取粉丝数量")
    @ApiResponse(description = "粉丝数量")
    @RequireJwt
    @GetMapping("/followers/count")
    fun getFollowerCount(): MaaResult<Long> {
        return success(userFlowService.getFollowerCount(helper.requireUserId()))
    }

    @Operation(summary = "检查是否已关注")
    @ApiResponse(description = "是否关注某个用户")
    @RequireJwt
    @GetMapping("/is-following/{targetUserId}")
    fun isFollowing(@PathVariable targetUserId: String): MaaResult<Boolean> {
        return success(userFlowService.isFollowing(helper.requireUserId(), targetUserId))
    }
}
