package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import plus.maa.backend.common.annotation.AccessLimit;
import plus.maa.backend.controller.response.ArkLevelInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.ArkLevelService;

import java.util.List;

/**
 * @author john180
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ArkLevelController {
    private final ArkLevelService arkLevelService;

    @GetMapping("/arknights/level")
    public MaaResult<List<ArkLevelInfo>> getLevels() {
        return MaaResult.success(arkLevelService.getArkLevelInfos());
    }

    @GetMapping("/test")
    @AccessLimit(times = 2,second=100)
    public MaaResult getLevels111() {
        return MaaResult.success("122321",null);
    }
}
