package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.model.ArkLevelType;
import plus.maa.backend.service.model.parser.ArkLevelParser;

import java.util.List;

/**
 * @author john180
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArkLevelParserService {
    private final List<ArkLevelParser> parsers;

    /**
     * 具体地图信息生成规则见
     * <a href="https://github.com/MaaAssistantArknights/MaaCopilotServer/blob/main/src/MaaCopilotServer.GameData/GameDataParser.cs">GameDataParser</a>
     * 尚未全部实现 <br>
     * TODO 完成剩余字段实现
     */
    @Nullable
    public ArkLevel parseLevel(ArkTilePos tilePos, String sha) {
        ArkLevel level = ArkLevel.builder()
                .levelId(tilePos.getLevelId())
                .stageId(tilePos.getStageId())
                .sha(sha)
                .catThree(tilePos.getCode())
                .name(tilePos.getName())
                .width(tilePos.getWidth())
                .height(tilePos.getHeight())
                .build();
        return parseLevel(level, tilePos);
    }

    private ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        ArkLevelType type = ArkLevelType.fromLevelId(level.getLevelId());
        if (ArkLevelType.UNKNOWN == type) {
            log.warn("[PARSER]未知关卡类型:{}", level.getLevelId());
            return null;
        }
        ArkLevelParser parser = parsers.stream()
                .filter(p -> p.supportType(type))
                .findFirst()
                .orElse(null);
        if (parser == null) {
            //类型存在但无对应Parser直接跳过
            return ArkLevel.EMPTY;
        }
        return parser.parseLevel(level, tilePos);
    }
}
