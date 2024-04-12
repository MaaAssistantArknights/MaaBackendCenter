package plus.maa.backend.config

import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration(proxyBeanMethods = false)
class ThreadPoolConfig {
    @Lazy
    @Primary
    @Bean(
        name = [
            TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME,
        ],
    )
    fun defaultTaskExecutor(builder: ThreadPoolTaskExecutorBuilder): ThreadPoolTaskExecutor = builder.build()

    @Bean
    fun emailTaskExecutor(): ThreadPoolTaskExecutor {
        // 在默认线程池配置的基础上修改了核心线程数和线程名称
        val taskExecutor = ThreadPoolTaskExecutor()
        // I/O 密集型配置
        taskExecutor.corePoolSize = Runtime.getRuntime().availableProcessors() * 2
        taskExecutor.setThreadNamePrefix("email-task-")
        // 动态的核心线程数量
        taskExecutor.setAllowCoreThreadTimeOut(true)
        return taskExecutor
    }
}
