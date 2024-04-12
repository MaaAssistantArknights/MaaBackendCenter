package plus.maa.backend.service.model.parser

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.ArkGameDataService
import plus.maa.backend.service.model.ArkLevelType
import java.util.Locale

private val log = KotlinLogging.logger { }

/**
 * @author john180
 *
 *
 * Memory level will be tagged like this:<br></br>
 * MEMORY -> POSITION -> OPERATOR_NAME == obt/memory/LEVEL_ID<br></br>
 * eg:<br></br>
 * 悖论模拟 -> 狙击 -> 克洛丝 == obt/memory/level_memory_kroos_1<br></br>
 */
@Component
class MemoryParser(
    val dataService: ArkGameDataService,
) : ArkLevelParser {
    override fun supportType(type: ArkLevelType): Boolean = ArkLevelType.MEMORY == type

    override fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        level.catOne = ArkLevelType.MEMORY.display

        val chIdSplit =
            level
                .stageId!!
                .split("_".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray() // mem_aurora_1
        if (chIdSplit.size != 3) {
            log.error { "[PARSER]悖论模拟关卡stageId异常: ${level.stageId}, level: ${level.levelId}" }
            return null
        }
        val chId = chIdSplit[1] // aurora
        val character = dataService.findCharacter(chId)
        if (character == null) {
            log.error { "[PARSER]悖论模拟关卡未找到角色信息: ${level.stageId}, level: ${level.levelId}" }
            return null
        }

        level.catTwo = parseProfession(character.profession)
        level.catThree = character.name

        return level
    }

    private fun parseProfession(professionId: String): String = when (professionId.lowercase(Locale.getDefault())) {
        "medic" -> "医疗"
        "special" -> "特种"
        "warrior" -> "近卫"
        "sniper" -> "狙击"
        "tank" -> "重装"
        "caster" -> "术师"
        "pioneer" -> "先锋"
        "support" -> "辅助"
        else -> "未知"
    }
}
