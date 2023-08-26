package plus.maa.backend.config;

import cn.hutool.dfa.WordTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 敏感词配置类 <br>
 *
 * @author lixuhuilll
 * Date: 2023-08-25 18:50
 */

@Slf4j
@Configuration
public class SensitiveWordConfig {

    // 标准的 Spring 路径匹配语法，默认为 classpath:sensitive-word.txt
    @Value("${maa-copilot.sensitive-word.path:classpath:sensitive-word.txt}")
    String sensitiveWordPath;

    /**
     * 敏感词库初始化 <br>
     * 使用 Hutool 的 DFA 算法库，如果后续需要可转其他开源库或者使用付费的敏感词库 <br>
     *
     * @return 敏感词库
     */

    @Bean
    public WordTree sensitiveWordInit(ApplicationContext applicationContext) throws IOException {
        // Spring 上下文获取敏感词文件
        Resource sensitiveWordResource = applicationContext.getResource(sensitiveWordPath);
        WordTree wordTree = new WordTree();

        // 获取载入用时
        long start = System.currentTimeMillis();

        // 以行为单位载入敏感词
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(sensitiveWordResource.getInputStream()))
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                wordTree.addWord(line);
            }
        } catch (Exception e) {
            log.error("敏感词库初始化失败：{}", e.getMessage());
            throw e;
        }

        log.info("敏感词库初始化完成，耗时 {} ms", System.currentTimeMillis() - start);

        return wordTree;
    }
}
