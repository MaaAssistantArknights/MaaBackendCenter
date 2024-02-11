package plus.maa.backend.config.external


data class ArkLevelGit(
        var repository: String = "https://github.com/MaaAssistantArknights/MaaAssistantArknights.git",
        var localRepository: String = "./MaaAssistantArknights",
        var jsonPath: String = "resource/Arknights-Tile-Pos/",
)
