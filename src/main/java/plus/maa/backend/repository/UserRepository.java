package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import plus.maa.backend.repository.entity.MaaUser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author AnselYuki
 */
public interface UserRepository extends MongoRepository<MaaUser, String> {
    /**
     * 根据邮箱（用户唯一登录凭据）查询
     *
     * @param email 邮箱字段
     * @return 查询用户
     */
    MaaUser findByEmail(String email);

    default Map<String, MaaUser> findByUsersId(List<String> userId) {
        return findAllById(userId)
                .stream().collect(Collectors.toMap(MaaUser::getUserId, Function.identity()));
    }
}
