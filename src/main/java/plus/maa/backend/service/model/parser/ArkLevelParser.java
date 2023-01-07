package plus.maa.backend.service.model.parser;

import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.model.ArkLevelType;

/**
 * @author john180
 */
public interface ArkLevelParser {

    /**
     * 是否支持解析该关卡类型
     *
     * @param type 关卡类型
     * @return 是否支持
     */
    boolean supportType(ArkLevelType type);

    /**
     * 解析关卡
     */
    ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos);

}
