package plus.maa.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.common.utils.ArkLevelUtil;
import plus.maa.backend.repository.entity.gamedata.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author john180
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArkGameDataService {
    private static final String ARK_STAGE = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/stage_table.json";
    private static final String ARK_ZONE = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/zone_table.json";
    private static final String ARK_ACTIVITY = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/activity_table.json";
    private static final String ARK_CHARACTER = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/character_table.json";
    private static final String ARK_TOWER = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/climb_tower_table.json";
    private static final String ARK_CRISIS_V2 = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/crisis_v2_table.json";
    private final OkHttpClient okHttpClient;
    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    private Map<String, ArkStage> stageMap = new ConcurrentHashMap<>();
    private Map<String, ArkStage> levelStageMap = new ConcurrentHashMap<>();
    private Map<String, ArkZone> zoneMap = new ConcurrentHashMap<>();
    private Map<String, ArkActivity> zoneActivityMap = new ConcurrentHashMap<>();
    private Map<String, ArkCharacter> arkCharacterMap = new ConcurrentHashMap<>();
    private Map<String, ArkTower> arkTowerMap = new ConcurrentHashMap<>();
    private Map<String, ArkCrisisV2Info> arkCrisisV2InfoMap = new ConcurrentHashMap<>();

    public void syncGameData() {
        syncStage();
        syncZone();
        syncActivity();
        syncCharacter();
        syncTower();
        syncCrisisV2Info();
    }

    @Nullable
    public ArkStage findStage(String levelId, String code, String stageId) {
        ArkStage stage = levelStageMap.get(levelId.toLowerCase());
        if (stage != null && stage.getCode().equalsIgnoreCase(code)) {
            return stage;
        }
        return stageMap.get(stageId);
    }

    @Nullable
    public ArkZone findZone(String levelId, String code, String stageId) {
        ArkStage stage = findStage(levelId, code, stageId);
        if (stage == null) {
            log.error("[DATA]stage不存在:{}, Level: {}", stageId, levelId);
            return null;
        }
        ArkZone zone = zoneMap.get(stage.getZoneId());
        if (zone == null) {
            log.error("[DATA]zone不存在:{}, Level: {}", stage.getZoneId(), levelId);
        }
        return zone;
    }

    @Nullable
    public ArkTower findTower(String zoneId) {
        return arkTowerMap.get(zoneId);
    }

    @Nullable
    public ArkCharacter findCharacter(String characterId) {
        String[] ids = characterId.split("_");
        return arkCharacterMap.get(ids[ids.length - 1]);
    }

    @Nullable
    public ArkActivity findActivityByZoneId(String zoneId) {
        return zoneActivityMap.get(zoneId);
    }

    /**
     * 通过 stageId 或者 seasonId 提取危机合约信息
     *
     * @param id stageId 或者 seasonId
     * @return 危机合约信息，包含合约名、开始时间、结束时间等
     */
    @Nullable
    public ArkCrisisV2Info findCrisisV2InfoById(String id) {
        return findCrisisV2InfoByKeyInfo(ArkLevelUtil.getKeyInfoById(id));
    }

    /**
     * 通过地图系列的唯一标识提取危机合约信息
     *
     * @param keyInfo 地图系列的唯一标识
     * @return 危机合约信息，包含合约名、开始时间、结束时间等
     */
    @Nullable
    public ArkCrisisV2Info findCrisisV2InfoByKeyInfo(String keyInfo) {
        return arkCrisisV2InfoMap.get(keyInfo);
    }

    private void syncStage() {
        Request req = new Request.Builder().url(ARK_STAGE).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]获取stage数据失败");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());
            JsonNode stagesNode = node.get("stages");
            Map<String, ArkStage> temp = mapper.convertValue(stagesNode, new TypeReference<>() {
            });
            stageMap = new ConcurrentHashMap<>();
            levelStageMap = new ConcurrentHashMap<>();
            temp.forEach((k, v) -> {
                stageMap.put(k, v);
                if (!ObjectUtils.isEmpty(v.getLevelId())) {
                    levelStageMap.put(v.getLevelId().toLowerCase(), v);
                }
            });

            log.info("[DATA]获取stage数据成功, 共{}条", levelStageMap.size());
        } catch (Exception e) {
            log.error("[DATA]同步stage数据异常", e);
        }
    }

    private void syncZone() {
        Request req = new Request.Builder().url(ARK_ZONE).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]获取zone数据失败");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());
            JsonNode zonesNode = node.get("zones");
            Map<String, ArkZone> temp = mapper.convertValue(zonesNode, new TypeReference<>() {
            });
            zoneMap = new ConcurrentHashMap<>(temp);
            log.info("[DATA]获取zone数据成功, 共{}条", zoneMap.size());
        } catch (Exception e) {
            log.error("[DATA]同步zone数据异常", e);
        }
    }

    private void syncActivity() {
        Request req = new Request.Builder().url(ARK_ACTIVITY).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]获取activity数据失败");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());

            //zoneId转换活动Id
            JsonNode zonesNode = node.get("zoneToActivity");
            Map<String, String> zoneToActivity = mapper.convertValue(zonesNode, new TypeReference<>() {
            });
            //活动信息
            JsonNode baseInfoNode = node.get("basicInfo");
            Map<String, ArkActivity> baseInfos = mapper.convertValue(baseInfoNode, new TypeReference<>() {
            });
            Map<String, ArkActivity> temp = new ConcurrentHashMap<>();
            zoneToActivity.forEach((zoneId, actId) -> {
                ArkActivity act = baseInfos.get(actId);
                if (act != null) {
                    temp.put(zoneId, act);
                }
            });
            zoneActivityMap = temp;

            log.info("[DATA]获取activity数据成功, 共{}条", zoneActivityMap.size());
        } catch (Exception e) {
            log.error("[DATA]同步activity数据异常", e);
        }
    }

    private void syncCharacter() {
        Request req = new Request.Builder().url(ARK_CHARACTER).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]获取character数据失败");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());
            Map<String, ArkCharacter> characters = mapper.convertValue(node, new TypeReference<>() {
            });
            characters.forEach((id, c) -> c.setId(id));
            Map<String, ArkCharacter> temp = new ConcurrentHashMap<>();
            characters.values().forEach(c -> {
                if (ObjectUtils.isEmpty(c.getId())) {
                    return;
                }
                String[] ids = c.getId().split("_");
                if (ids.length != 3) {
                    //不是干员
                    return;
                }
                temp.put(ids[2], c);
            });
            arkCharacterMap = temp;

            log.info("[DATA]获取character数据成功, 共{}条", arkCharacterMap.size());
        } catch (Exception e) {
            log.error("[DATA]同步character数据异常", e);
        }
    }

    private void syncTower() {
        Request req = new Request.Builder().url(ARK_TOWER).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]获取tower数据失败");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());
            JsonNode towerNode = node.get("towers");
            arkTowerMap = mapper.convertValue(towerNode, new TypeReference<>() {
            });
            log.info("[DATA]获取tower数据成功, 共{}条", arkTowerMap.size());
        } catch (Exception e) {
            log.error("[DATA]同步tower数据异常", e);
        }
    }

    public void syncCrisisV2Info() {
        Request req = new Request.Builder().url(ARK_CRISIS_V2).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (!rsp.isSuccessful() || body == null) {
                log.error("[DATA]获取crisisV2Info数据失败");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.charStream());
            JsonNode crisisV2InfoNode = node.get("seasonInfoDataMap");
            Map<String, ArkCrisisV2Info> crisisV2InfoMap = mapper.convertValue(crisisV2InfoNode, new TypeReference<>() {
            });
            Map<String, ArkCrisisV2Info> temp = new ConcurrentHashMap<>();

            crisisV2InfoMap.forEach((k, v) -> temp.put(ArkLevelUtil.getKeyInfoById(k), v));
            arkCrisisV2InfoMap = temp;

            log.info("[DATA]获取crisisV2Info数据成功, 共{}条", arkCrisisV2InfoMap.size());
        } catch (Exception e) {
            log.error("[DATA]同步crisisV2Info数据异常", e);
        }
    }
}
