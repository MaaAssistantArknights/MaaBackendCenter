package plus.maa.backend.repository.entity.arklevel.parse;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;

@Component
public class UnknownLevelParse extends LevelParse {
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("未知类型" + LevelParse.parseType(level.getLevelId()));
    }
}
