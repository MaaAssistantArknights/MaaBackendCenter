package plus.maa.backend.repository.entity.arklevel;

import jakarta.annotation.Resource;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.ArkGameDataService;

public abstract class LevelParse {
    protected ArkGameDataService dataService;

    @Resource
    public void setDataService(ArkGameDataService dataService) {
        this.dataService = dataService;
    }

    public static String parseType(String levelId) {
        String[] ids = levelId.toLowerCase().split("/");
        return (ids[0].equals("obt")) ? ids[1] : ids[0];
    }

    public abstract void parse(ArkLevel level, ArkTilePos tilePos);
}
