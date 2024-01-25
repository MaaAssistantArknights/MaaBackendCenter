package plus.maa.backend.config.external


data class CopilotBackup(
        /**
         * 是否禁用备份功能
         */
        var disabled: Boolean = false,

        /**
         * 本地备份地址
         * */
        var dir: String = "/home/dove/copilotBak",

        /**
         * 远程备份地址
         */
        var uri: String = "git@github.com:dragove/maa-copilot-store.git",

        /**
         * git 用户名
         */
        var username: String = "dragove",

        /**
         * git 邮箱
         */
        var email: String = "dragove@qq.com",
)
