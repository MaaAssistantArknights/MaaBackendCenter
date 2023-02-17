package plus.maa.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import plus.maa.backend.repository.entity.Copilot;

/**
 * @author LoMu
 * Date  2022-12-27 10:28
 */

public interface CopilotRepository extends MongoRepository<Copilot, String> {

    Optional<Copilot> findFirstByOrderByCopilotIdDesc();

    Optional<Copilot> findByCopilotId(Long copilotId);

    boolean existsCopilotsByCopilotId(Long copilotId);

}
