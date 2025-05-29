package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.fail
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
    fun getFollowingList(
        @RequestParam page: Int = 1,
        @RequestParam @Max(value = 50, message = "单页大小不得超过50") size: Int = 10): MaaResult<Page<MaaUserInfo>> {
        // 之前的API约定分页从1开始，与Spring默认约定不同，在此转换
        if (page < 1) {
            return fail(422, "页数请从1开始")
        }
        val realPageable = PageRequest.of(
            page - 1,
            size,
        )
        return success(
            userFollowService.getFollowingList(helper.requireUserId(), realPageable),
        )
    }

    @Operation(summary = "获取粉丝列表")
    @ApiResponse(description = "粉丝列表")
    @RequireJwt
    @GetMapping("/fansList")
    fun getFansList(
        @RequestParam page: Int = 1,
        @RequestParam @Max(value = 50, message = "单页大小不得超过50") size: Int = 10): MaaResult<Page<MaaUserInfo>> {
        // 之前的API约定分页从1开始，与Spring默认约定不同，在此转换
        if (page < 1) {
            return fail(422, "页数请从1开始")
        }
        val realPageable = PageRequest.of(
            page - 1,
            size,
        )
        return success(
            userFollowService.getFansList(helper.requireUserId(), realPageable),
        )
    }
}
