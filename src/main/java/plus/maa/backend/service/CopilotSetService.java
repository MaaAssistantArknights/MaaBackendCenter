package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.utils.IdComponent;
import plus.maa.backend.common.utils.converter.CopilotSetConverter;
import plus.maa.backend.controller.request.copilotset.CopilotSetCreateReq;
import plus.maa.backend.repository.CopilotSetRepository;
import plus.maa.backend.repository.entity.CopilotSet;

/**
 * @author dragove
 * create on 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotSetService {

    private final IdComponent idComponent;
    private final CopilotSetConverter converter;
    private final CopilotSetRepository repository;

    public Long create(CopilotSetCreateReq req, String userId) {
        long id = idComponent.getId(CopilotSet.META);
        CopilotSet newCopilotSet =
                converter.convert(req, id, userId);
        repository.insert(newCopilotSet);
        return id;
    }
}
