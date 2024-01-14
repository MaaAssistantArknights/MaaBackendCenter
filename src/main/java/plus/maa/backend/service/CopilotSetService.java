package plus.maa.backend.service;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.utils.IdComponent;
import plus.maa.backend.common.utils.converter.CopilotSetConverter;
import plus.maa.backend.controller.request.CopilotSetQuery;
import plus.maa.backend.controller.request.CopilotSetUpdateReq;
import plus.maa.backend.controller.request.copilotset.CopilotSetCreateReq;
import plus.maa.backend.controller.request.copilotset.CopilotSetModCopilotsReq;
import plus.maa.backend.controller.response.CopilotSetPageRes;
import plus.maa.backend.controller.response.CopilotSetRes;
import plus.maa.backend.repository.CopilotSetRepository;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.CopilotSet;
import plus.maa.backend.repository.entity.MaaUser;

import java.time.LocalDateTime;
import java.util.*;

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
    private final UserRepository userRepository;
    private final Sort DEFAULT_SORT = Sort.by("id").descending();

    /**
     * 创建作业集
     *
     * @param req    作业集创建请求
     * @param userId 创建者用户id
     * @return 作业集id
     */
    public long create(CopilotSetCreateReq req, String userId) {
        long id = idComponent.getId(CopilotSet.META);
        CopilotSet newCopilotSet =
                converter.convert(req, id, userId);
        repository.insert(newCopilotSet);
        return id;
    }

    /**
     * 往作业集中加入作业id列表
     */
    public void addCopilotIds(CopilotSetModCopilotsReq req, String userId) {
        CopilotSet copilotSet = repository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("作业集不存在"));
        Assert.state(copilotSet.getCreatorId().equals(userId), "您不是该作业集的创建者，无权修改该作业集");
        copilotSet.getCopilotIds().addAll(req.getCopilotIds());
        copilotSet.setCopilotIds(copilotSet.getDistinctIdsAndCheck());
        repository.save(copilotSet);
    }

    /**
     * 往作业集中删除作业id列表
     */
    public void removeCopilotIds(CopilotSetModCopilotsReq req, String userId) {
        CopilotSet copilotSet = repository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("作业集不存在"));
        Assert.state(copilotSet.getCreatorId().equals(userId), "您不是该作业集的创建者，无权修改该作业集");
        Set<Long> removeIds = new HashSet<>(req.getCopilotIds());
        copilotSet.getCopilotIds().removeIf(removeIds::contains);
        repository.save(copilotSet);
    }

    /**
     * 更新作业集信息
     */
    public void update(CopilotSetUpdateReq req, String userId) {
        CopilotSet copilotSet = repository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("作业集不存在"));
        Assert.state(copilotSet.getCreatorId().equals(userId), "您不是该作业集的创建者，无权修改该作业集");
        copilotSet.setName(req.getName());
        copilotSet.setDescription(req.getDescription());
        copilotSet.setStatus(req.getStatus());
        repository.save(copilotSet);
    }

    /**
     * 删除作业集信息（逻辑删除，保留详情接口查询结果）
     *
     * @param id     作业集id
     * @param userId 登陆用户id
     */
    public void delete(long id, String userId) {
        log.info("delete copilot set for id: {}, userId: {}", id, userId);
        CopilotSet copilotSet = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("作业集不存在"));
        Assert.state(copilotSet.getCreatorId().equals(userId), "您不是该作业集的创建者，无权删除该作业集");
        copilotSet.setDelete(true);
        copilotSet.setDeleteTime(LocalDateTime.now());
        repository.save(copilotSet);
    }

    public CopilotSetPageRes query(CopilotSetQuery req) {
        PageRequest pageRequest = PageRequest.of(req.getPage() - 1, req.getLimit(), DEFAULT_SORT);

        String keyword = req.getKeyword();
        Page<CopilotSet> copilotSets;
        if (StringUtils.isBlank(keyword)) {
            copilotSets = repository.findAll(pageRequest);
        } else {
            copilotSets = repository.findByKeyword(keyword, pageRequest);
        }

        List<String> userIds = copilotSets.stream().map(CopilotSet::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, MaaUser> userById = userRepository.findByUsersId(userIds);
        return new CopilotSetPageRes()
                .setPage(copilotSets.getNumber() + 1)
                .setTotal(copilotSets.getTotalElements())
                .setHasNext(copilotSets.getTotalPages() > req.getPage())
                .setData(copilotSets.stream().map(cs -> {
                    MaaUser user = userById.getOrDefault(cs.getCreatorId(), MaaUser.UNKNOWN);
                    return converter.convert(cs, user.getUserName());
                }).toList());

    }

    public CopilotSetRes get(long id) {
        return repository.findById(id).map($ -> {
            String userName = userRepository.findByUserId($.getCreatorId()).orElse(MaaUser.UNKNOWN).getUserName();
            return converter.convertDetail($, userName);
        }).orElseThrow(() -> new IllegalArgumentException("作业不存在"));
    }
}
