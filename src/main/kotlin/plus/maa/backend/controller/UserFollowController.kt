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
import plus.maa.backend.service.UserFollowService

@RestController
@RequestMapping("/follow")
@Tag(name = "UserFollow", description = "用户关注管理接口")
class UserFollowController(
    private val userFollowService: UserFollowService,
    private val helper: AuthenticationHelper,
) {

    @Operation(summary = "关注用户")
    @ApiResponse(description = "关注结果")
    @RequireJwt
    @PostMapping("/follow/{followUserId}")
    fun follow(@PathVariable followUserId: String): MaaResult<Unit> = success(
        userFollowService.follow(helper.requireUserId(), followUserId),
    )

    @Operation(summary = "取消关注")
    @ApiResponse(description = "取消关注结果")
    @RequireJwt
    @PostMapping("/unfollow/{followUserId}")
    fun unfollow(@PathVariable followUserId: String): MaaResult<Unit> = success(
        userFollowService.unfollow(helper.requireUserId(), followUserId),
    )

    @Operation(summary = "获取关注列表")
    @ApiResponse(description = "关注列表")
    @RequireJwt
    @GetMapping("/followingList")
    fun getFollowingList(pageable: Pageable): MaaResult<Page<MaaUserInfo>> = success(
        userFollowService.getFollowingList(helper.requireUserId(), pageable),
    )

    @Operation(summary = "获取粉丝列表")
    @ApiResponse(description = "粉丝列表")
    @RequireJwt
    @GetMapping("/fansList")
    fun getFansList(pageable: Pageable): MaaResult<Page<MaaUserInfo>> = success(
        userFollowService.getFansList(helper.requireUserId(), pageable),
    )
}
