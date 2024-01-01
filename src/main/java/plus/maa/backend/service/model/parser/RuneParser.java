package plus.maa.backend.service.model.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkCrisisV2Info;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.ArkGameDataService;
import plus.maa.backend.service.model.ArkLevelType;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RuneParser implements ArkLevelParser {

    private final ArkGameDataService dataService;

    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.RUNE.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.RUNE.getDisplay());
        level.setCatTwo(
                Optional.ofNullable(level.getStageId())
                        .map(dataService::findCrisisV2InfoById)
                        .map(ArkCrisisV2Info::getName)
                        .orElse(tilePos.getCode())
        );
        level.setCatThree(level.getName());
        return level;
    }
}
