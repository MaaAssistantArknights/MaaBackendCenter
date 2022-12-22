package plus.maa.backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import plus.maa.backend.model.MaaUser;

public interface MaaUserRepository extends MongoRepository<MaaUser, String> {

}