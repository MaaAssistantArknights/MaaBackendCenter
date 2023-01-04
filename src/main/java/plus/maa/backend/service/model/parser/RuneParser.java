package plus.maa.backend.service.model.parser;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.model.ArkLevelType;

@Component
public class RuneParser implements ArkLevelParser {
    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.RUNE.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.RUNE.getDisplay());
        level.setCatTwo(tilePos.getCode());
        level.setCatThree(level.getName());
        return level;
    }
}
