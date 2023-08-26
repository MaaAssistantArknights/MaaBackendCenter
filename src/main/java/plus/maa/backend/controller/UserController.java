package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import plus.maa.backend.config.SpringDocConfig;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.config.security.AuthenticationHelper;
import plus.maa.backend.controller.request.user.RefreshReq;
import plus.maa.backend.controller.request.user.UserInfoUpdateDTO;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.user.MaaLoginRsp;
import plus.maa.backend.service.EmailService;
import plus.maa.backend.service.UserService;

/**
 * 用户相关接口
 * <a href=
 * "https://github.com/MaaAssistantArknights/maa-copilot-frontend/blob/dev/src/apis/auth.ts">前端api约定文件</a>
 *
 * @author AnselYuki
 */
@Data
@Tag(name = "CopilotUser", description = "用户管理")
@RequestMapping("/user")
@Validated
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final EmailService emailService;
    private final MaaCopilotProperties properties;
    private final AuthenticationHelper helper;
    @Value("${maa-copilot.jwt.header}")
    private String header;



    /**
     * 更新用户详细信息
     *
     * @param updateDTO 用户信息参数
     * @return http响应
     */
    @Operation(summary = "更新用户详细信息")
    @ApiResponse(description = "更新结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/update/info")
    public MaaResult<Void> updateInfo(
            @Parameter(description = "更新用户详细信息请求") @Valid @RequestBody UserInfoUpdateDTO updateDTO
    ) {
        userService.updateUserInfo(helper.requireUserId(), updateDTO);
        return MaaResult.success();
    }



    /**
     * 刷新token
     *
     * @param request http请求，用于获取请求头
     * @return 成功响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新token")
    @ApiResponse(description = "刷新token结果")
    public MaaResult<MaaLoginRsp> refresh(@Parameter(description = "刷新token请求") @RequestBody RefreshReq request) {
        var res = userService.refreshToken(request.getRefreshToken());
        return MaaResult.success(res);
    }


    /**
     *  授权用户登录
     *
     * @return 成功响应，荷载JwtToken
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @ApiResponse(description = "登录结果")
    public MaaResult<MaaLoginRsp> login() {
        return MaaResult.success("登陆成功", userService.login(helper.getUserNameAndEmail()));
    }


}
