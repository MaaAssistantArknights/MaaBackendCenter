package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.MaaUser;

/**
 * @author AnselYuki
 */
@Repository
public interface UserRepository extends MongoRepository<MaaUser, String> {
    /**
     * 根据邮箱（用户唯一登录凭据）查询
     * @param email 邮箱字段
     * @return 查询用户
     */
    MaaUser findByEmail(String email);
}
