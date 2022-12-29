package plus.maa.backend.repository.entity.arklevel.parse;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;

@Component
public class RuneLevelParse extends LevelParse {
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("危机合约");
        level.setCatTwo(tilePos.getCode());
        level.setCatThree(level.getName());
    }
}
