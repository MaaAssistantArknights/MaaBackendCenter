package plus.maa.backend.repository.entity.gamedata;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 地图格子数据
 *
 * @author dragove
 * created on 2022/12/23
 */
@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ArkTilePos {

    private String code;
    private Integer height;
    private Integer width;
    private String levelId;
    private String name;
    private String stageId;
    private List<List<Tile>> tiles;
    private List<List<Double>> view;

    @Data
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Tile {
        private String tileKey;
        private Integer heightType;
        private Integer buildableType;
        private Boolean isStart;
        private Boolean isEnd;
    }
}
