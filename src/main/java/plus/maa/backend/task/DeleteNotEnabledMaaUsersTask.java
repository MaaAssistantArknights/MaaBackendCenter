package plus.maa.backend.task;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.UserRepository;


/**
 * 定期删除未激活的账户，释放数据库空间
 *
 * @author Lixuhuilll
 * created on 2023.09.17
 */
@Component
@RequiredArgsConstructor
public class DeleteNotEnabledMaaUsersTask {

    final UserRepository userRepository;

    /**
     * 每三天的凌晨 0 点整清理一次
     */
    @Scheduled(cron = "0 0 0 */3 * ?")
    public void deleteNotEnabledMaaUsers() {
        userRepository.deleteAllByStatusIs(0);
    }
}
