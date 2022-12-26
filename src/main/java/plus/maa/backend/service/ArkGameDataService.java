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
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkZone;

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
    private final OkHttpClient okHttpClient;
    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    private Map<String, ArkStage> stageMap = new ConcurrentHashMap<>();
    private Map<String, ArkStage> levelStageMap = new ConcurrentHashMap<>();
    private Map<String, ArkZone> zoneMap = new ConcurrentHashMap<>();

    public void syncGameData() {
        syncStage();
        syncZone();
    }

    public ArkStage findStage(String levelId, String code, String stageId) {
        ArkStage stage = levelStageMap.get(levelId.toLowerCase());
        if (stage != null && stage.getCode().equalsIgnoreCase(code)) {
            return stage;
        }
        return stageMap.get(stageId);
    }

    public ArkZone getZone(String zoneId) {
        return zoneMap.get(zoneId);
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
}
