package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.GithubRepository;
import plus.maa.backend.repository.entity.ArknightsTilePos;
import plus.maa.backend.repository.entity.GithubContent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dragove
 * created on 2022/12/23
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GithubService {

    /**
     * Github api调用token 从 <a href="https://github.com/settings/tokens">tokens</a> 获取
     */
    @Value("${maa-copilot.github.token:}")
    private String githubToken;

    /**
     * maa 主仓库，一般不变
     */
    @Value("${maa-copilot.github.repo:MaaAssistantArknights/MaaAssistantArknights}")
    private String maaRepo;

    /**
     * 地图数据所在路径
     */
    @Value("${maa-copilot.github.repo.tile.path:resource/Arknights-Tile-Pos}")
    private String tilePosPath;

    private final GithubRepository githubRepository;

    /**
     * 获取maa所有地图数据
     *
     * @return maa 地图数据列表
     */
    public List<ArknightsTilePos> getAllTilePos() {
        List<GithubContent> contents = getArknightsTilePosPaths();
        List<ArknightsTilePos> tilePosList = new ArrayList<>(contents.size());
        try {
            for (GithubContent content : contents) {
                if (content.isDir() || content.getFileExtension().equals("json")) {
                    continue;
                }
                tilePosList.add(githubRepository.downloadArknightsTilePos(new URI(content.getDownloadUrl())));
            }
        } catch (URISyntaxException e) {
            log.error("meet unexpected uri syntax, msg: {}", e.getMessage());
        }
        return tilePosList;
    }

    private List<GithubContent> getArknightsTilePosPaths() {
        return githubRepository.getContents(githubToken, maaRepo, tilePosPath);
    }


}
