package plus.maa.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.model.ArkLevelType
import plus.maa.backend.service.model.parser.ArkLevelParser

private val log = KotlinLogging.logger {  }

/**
 * @author john180
 */
@Service
class ArkLevelParserService(private val parsers: List<ArkLevelParser>) {

    /**
     * 具体地图信息生成规则见
     * [GameDataParser](https://github.com/MaaAssistantArknights/MaaCopilotServer/blob/main/src/MaaCopilotServer.GameData/GameDataParser.cs)
     * 尚未全部实现 <br></br>
     * TODO 完成剩余字段实现
     */
    @Nullable
    fun parseLevel(tilePos: ArkTilePos, sha: String?): ArkLevel? {
        val level = ArkLevel.builder()
            .levelId(tilePos.levelId)
            .stageId(tilePos.stageId)
            .sha(sha)
            .catThree(tilePos.code)
            .name(tilePos.name)
            .width(tilePos.width)
            .height(tilePos.height)
            .build();
        return parseLevel(level, tilePos)
    }

    private fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        val type = ArkLevelType.fromLevelId(level.levelId)
        if (ArkLevelType.UNKNOWN == type) {
            log.warn { "[PARSER]未知关卡类型:${level.levelId}" }
            return null
        }
        val parser = parsers.stream()
            .filter { p: ArkLevelParser? -> p!!.supportType(type) }
            .findFirst()
            .orElse(null)
        if (parser == null) {
            //类型存在但无对应Parser直接跳过
            return ArkLevel.EMPTY
        }
        return parser.parseLevel(level, tilePos)
    }
}
