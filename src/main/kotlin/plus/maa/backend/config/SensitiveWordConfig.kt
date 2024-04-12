package plus.maa.backend.config

import cn.hutool.dfa.WordTree
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 敏感词配置类 <br></br>
 *
 * @author lixuhuilll
 * Date: 2023-08-25 18:50
 */
@Configuration
class SensitiveWordConfig(
    // 标准的 Spring 路径匹配语法，默认为 classpath:sensitive-word.txt
    @Value("\${maa-copilot.sensitive-word.path:classpath:sensitive-word.txt}") private val sensitiveWordPath: String,
) {
    private val log = KotlinLogging.logger {}

    /**
     * 敏感词库初始化 <br></br>
     * 使用 Hutool 的 DFA 算法库，如果后续需要可转其他开源库或者使用付费的敏感词库 <br></br>
     *
     * @return 敏感词库
     */
    @Bean
    @Throws(IOException::class)
    fun sensitiveWordInit(applicationContext: ApplicationContext): WordTree {
        // Spring 上下文获取敏感词文件
        val sensitiveWordResource = applicationContext.getResource(sensitiveWordPath)
        val wordTree = WordTree()

        // 获取载入用时
        val start = System.currentTimeMillis()

        // 以行为单位载入敏感词
        try {
            BufferedReader(
                InputStreamReader(sensitiveWordResource.inputStream),
            ).use { bufferedReader ->
                var line: String?
                while ((bufferedReader.readLine().also { line = it }) != null) {
                    wordTree.addWord(line)
                }
            }
        } catch (e: Exception) {
            log.error { "敏感词库初始化失败：${e.message}" }
            throw e
        }

        log.info { "敏感词库初始化完成，耗时 ${System.currentTimeMillis() - start} ms" }

        return wordTree
    }
}
