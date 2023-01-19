package plus.maa.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import plus.maa.backend.controller.response.ArkLevelInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.ArkLevelService;

/**
 * @author john180
 */
@RestController
@RequiredArgsConstructor
public class ArkLevelController {
    private final ArkLevelService arkLevelService;

    @GetMapping("/arknights/level")
    public MaaResult<List<ArkLevelInfo>> getLevels() {
        return MaaResult.success(arkLevelService.getArkLevelInfos());
    }
}
