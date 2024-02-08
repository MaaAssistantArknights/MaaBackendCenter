package plus.maa.backend.config;

import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;

@Configuration(proxyBeanMethods = false)
public class ThreadPoolConfig {

    @Lazy
    @Primary
    @Bean(name = {APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME})
    public ThreadPoolTaskExecutor defaultTaskExecutor(TaskExecutorBuilder builder) {
        return builder.build();
    }

    @Bean
    public ThreadPoolTaskExecutor emailTaskExecutor() {
        // 在默认线程池配置的基础上修改了核心线程数和线程名称
        var taskExecutor = new ThreadPoolTaskExecutor();
        // I/O 密集型配置
        taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        taskExecutor.setThreadNamePrefix("email-task-");
        // 动态的核心线程数量
        taskExecutor.setAllowCoreThreadTimeOut(true);
        return taskExecutor;
    }
}
