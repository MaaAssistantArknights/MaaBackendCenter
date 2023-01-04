package plus.maa.backend.service.model.parser;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.model.ArkLevelType;

@Component
public class UnknownParser implements ArkLevelParser {
    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.UNKNOWN.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        String[] ids = level.getLevelId().toLowerCase().split("/");
        String type = (ids[0].equals("obt")) ? ids[1] : ids[0];

        level.setCatOne(ArkLevelType.UNKNOWN.getDisplay() + type);
        return level;
    }
}
