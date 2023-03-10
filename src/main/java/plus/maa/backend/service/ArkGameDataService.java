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
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
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
    private static final String ARK_STAGE = "https://raw.githubusercontent.com/Kengxxiao/ArknightsGameData/master/zh_CN/gamedata/excel/stage_table.json";
    private static final String ARK_ZONE = "https://raw.githubusercontent.com/Kengxxiao/ArknightsGameData/master/zh_CN/gamedata/excel/zone_table.json";
    private static final String ARK_ACTIVITY = "https://raw.githubusercontent.com/Kengxxiao/ArknightsGameData/master/zh_CN/gamedata/excel/activity_table.json";
    private static final String ARK_CHARACTER = "https://raw.githubusercontent.com/Kengxxiao/ArknightsGameData/master/zh_CN/gamedata/excel/character_table.json";
    private static final String ARK_TOWER = "https://raw.githubusercontent.com/Kengxxiao/ArknightsGameData/master/zh_CN/gamedata/excel/climb_tower_table.json";
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

    public void syncGameData() {
        syncStage();
        syncZone();
        syncActivity();
        syncCharacter();
        syncTower();
    }

    public ArkStage findStage(String levelId, String code, String stageId) {
        ArkStage stage = levelStageMap.get(levelId.toLowerCase());
        if (stage != null && stage.getCode().equalsIgnoreCase(code)) {
            return stage;
        }
        return stageMap.get(stageId);
    }

    public ArkZone findZone(String levelId, String code, String stageId) {
        ArkStage stage = findStage(levelId, code, stageId);
        if (stage == null) {
            log.error("[DATA]stage?????????:{}, Level: {}", stageId, levelId);
            return null;
        }
        ArkZone zone = zoneMap.get(stage.getZoneId());
        if (zone == null) {
            log.error("[DATA]zone?????????:{}, Level: {}", stage.getZoneId(), levelId);
        }
        return zone;
    }

    public ArkTower findTower(String zoneId) {
        return arkTowerMap.get(zoneId);
    }

    public ArkCharacter findCharacter(String characterId) {
        String[] ids = characterId.split("_");
        return arkCharacterMap.get(ids[ids.length - 1]);
    }

    public ArkActivity findActivityByZoneId(String zoneId) {
        return zoneActivityMap.get(zoneId);
    }

    private void syncStage() {
        Request req = new Request.Builder().url(ARK_STAGE).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]??????stage????????????");
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

            log.info("[DATA]??????stage????????????, ???{}???", levelStageMap.size());
        } catch (Exception e) {
            log.error("[DATA]??????stage????????????", e);
        }
    }

    private void syncZone() {
        Request req = new Request.Builder().url(ARK_ZONE).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]??????zone????????????");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());
            JsonNode zonesNode = node.get("zones");
            Map<String, ArkZone> temp = mapper.convertValue(zonesNode, new TypeReference<>() {
            });
            zoneMap = new ConcurrentHashMap<>(temp);
            log.info("[DATA]??????zone????????????, ???{}???", zoneMap.size());
        } catch (Exception e) {
            log.error("[DATA]??????zone????????????", e);
        }
    }

    private void syncActivity() {
        Request req = new Request.Builder().url(ARK_ACTIVITY).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]??????activity????????????");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());

            //zoneId????????????Id
            JsonNode zonesNode = node.get("zoneToActivity");
            Map<String, String> zoneToActivity = mapper.convertValue(zonesNode, new TypeReference<>() {
            });
            //????????????
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

            log.info("[DATA]??????activity????????????, ???{}???", zoneActivityMap.size());
        } catch (Exception e) {
            log.error("[DATA]??????activity????????????", e);
        }
    }

    private void syncCharacter() {
        Request req = new Request.Builder().url(ARK_CHARACTER).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]??????character????????????");
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
                    //????????????
                    return;
                }
                temp.put(ids[2], c);
            });
            arkCharacterMap = temp;

            log.info("[DATA]??????character????????????, ???{}???", arkCharacterMap.size());
        } catch (Exception e) {
            log.error("[DATA]??????character????????????", e);
        }
    }

    private void syncTower() {
        Request req = new Request.Builder().url(ARK_TOWER).get().build();
        try (Response rsp = okHttpClient.newCall(req).execute()) {
            ResponseBody body = rsp.body();
            if (body == null) {
                log.error("[DATA]??????tower????????????");
                return;
            }
            JsonNode node = mapper.reader().readTree(body.string());
            JsonNode towerNode = node.get("towers");
            arkTowerMap = mapper.convertValue(towerNode, new TypeReference<>() {
            });
            log.info("[DATA]??????tower????????????, ???{}???", arkTowerMap.size());
        } catch (Exception e) {
            log.error("[DATA]??????tower????????????", e);
        }
    }
}
