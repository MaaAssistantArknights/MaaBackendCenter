package plus.maa.backend.repository.entity.gamedata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkCharacter {
    private String id;
    private String name;
    private String profession;
    private int rarity;
}
