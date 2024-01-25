package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.info.GitProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.response.MaaResult

/**
 * @author AnselYuki
 */
@Tag(name = "System", description = "系统管理接口")
@RequestMapping("")
@RestController
class SystemController(
        private val properties: MaaCopilotProperties,
        private val gitProperties: GitProperties?
) {

    /**
     * Tests if the server is ready.
     * @return 系统启动信息
     */
    @GetMapping("/")
    fun test(): MaaResult<String?> {
        return MaaResult.success("Maa Copilot Server is Running", null)
    }

    /**
     * Gets the current version of the server.
     * @return 系统版本信息
     */
    @GetMapping("version")
    fun getSystemVersion(): MaaResult<MaaSystemInfo> {
        val info = properties.info
        val systemInfo = MaaSystemInfo(info.title, info.description, info.version, gitProperties!!)
        return MaaResult.success(systemInfo)
    }

    data class MaaSystemInfo(
            val title: String,
            val description: String,
            val version: String,
            val git: GitProperties,
    )
}