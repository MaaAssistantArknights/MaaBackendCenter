package plus.maa.backend.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import plus.maa.backend.controller.request.copilot.CopilotDTO;
import plus.maa.backend.repository.entity.gamedata.*;
import plus.maa.backend.service.model.RatingCache;
import plus.maa.backend.service.session.UserSession;

/**
 * 添加所有需要用到反射的类到此处，用于 native image
 * 等个大佬修缮
 *
 * @author dragove
 * created on 2023/08/18
 */
@Configuration
@RegisterReflectionForBinding({
        ArkActivity.class, ArkCharacter.class, ArkStage.class,
        ArkTilePos.class, ArkTilePos.Tile.class, ArkTower.class,
        ArkZone.class, CopilotDTO.class, RatingCache.class,
        UserSession.class,
        PropertyNamingStrategies.SnakeCaseStrategy.class,
        PropertyNamingStrategies.LowerCamelCaseStrategy.class
})
public class NativeReflectionConfig {

}
