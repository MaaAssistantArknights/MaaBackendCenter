package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.model.MaaUser;

/**
 * @author AnselYuki
 */
@Repository
public interface UserRepository extends MongoRepository<MaaUser, String> {
}
