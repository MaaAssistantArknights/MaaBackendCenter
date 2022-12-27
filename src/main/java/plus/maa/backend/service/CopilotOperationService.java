package plus.maa.backend.service;

import cn.hutool.core.lang.ObjectId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import plus.maa.backend.controller.request.CopilotRequest;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.CopilotOperation;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.LoginUser;


import java.util.Objects;
import java.util.Optional;

/**
 * @author LoMu
 * Date  2022-12-25 19:57
 */
@Service
@RequiredArgsConstructor
public class CopilotOperationService {

    private final CopilotRepository copilotRepository;


    /**
     * @return 获取当前登录用户
     */
    private LoginUser getCurrentUser() {
        /* return (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();*/
        LoginUser loginUser = new LoginUser();
        loginUser.setMaaUser(new MaaUser().setUserId("66666").setUserName("halo"));
        return loginUser;
    }

    /**
     * 根据_id获取CopilotOperation
     *
     * @param id _id
     * @return CopilotOperation
     */
    private CopilotOperation findByid(String id) {
        Optional<CopilotOperation> optional = copilotRepository.findById(id);

        CopilotOperation copilot;
        if (optional.isPresent()) {
            copilot = optional.get();
        } else {
            throw new MaaResultException("作业id不存在");
        }
        return copilot;
    }

    /**
     * 无法对他人作业进行操作
     *
     * @param operationId 作业id
     * @return boolean
     */
    private Boolean verifyOwner(String operationId) {
        String userId = getCurrentUser().getMaaUser().getUserId();
        CopilotOperation copilotOperation = findByid(operationId);
        return Objects.equals(copilotOperation.getUploaderId(), userId);
    }


    public MaaResult<String> upload(CopilotOperation copilotOperation) {
        LoginUser user = getCurrentUser();
        String id = ObjectId.next();
        copilotOperation
                .setUploaderId(user.getMaaUser().getUserId())
                .setUploader(user.getMaaUser().getUserName())
                /* .setCreateDate(LocalDateTime.now())
                 .setUpdateDate(LocalDateTime.now())*/
                .setId(id);

        try {
            copilotRepository.insert(copilotOperation);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return MaaResult.success(id);
    }

    public MaaResult<Void> delete(CopilotRequest request) {
        String operationId = request.getId();

        if (verifyOwner(operationId)) {
            copilotRepository.deleteById(operationId);
            return MaaResult.success(null);
        } else {
            throw new MaaResultException("无法删除他人作业");
        }

    }


    public MaaResult<CopilotOperation> getCoplilotById(String id) {
        return MaaResult.success(findByid(id));
    }


    /**
     * 分页查询
     *
     * @param request 指定查询
     * @return CopilotPageInfo
     */
    public MaaResult<CopilotPageInfo> queriesCopilot(CopilotRequest request) {
        String orderby = "id";
        Sort.Order sortOrder = new Sort.Order(Sort.Direction.ASC, orderby);
        Integer page = 1;
        Integer limit = 10;
        boolean hasNext = false;

        if (request.getPage() != null && request.getPage() > 0) {
            page = request.getPage();
        }
        if (request.getLimit() != null && request.getLimit() > 0) {
            limit = request.getLimit();
        }
        if (request.getOrderby() != null && !Objects.equals(request.getOrderby(), "")) {
            orderby = request.getOrderby();
        }
        if (request.getDesc() != null && request.getDesc()) {
            sortOrder = new Sort.Order(Sort.Direction.DESC, orderby);
        }
        //分页排序
        Pageable pageable = PageRequest.of(
                page - 1, limit
                , Sort.by(sortOrder));

        CopilotOperation copilotOperation = new CopilotOperation();

        //模糊查询
        copilotOperation.setDoc(
                        new CopilotOperation.Doc()
                                .setTitle(request.getDocument())
                                .setDetails(request.getDocument()))
                .setUploader(request.getUploader())
                .setUploaderId(request.getUploaderId());
        Example<CopilotOperation> exampleObj = Example.of(
                copilotOperation, ExampleMatcher.matching()
                        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                        .withIgnoreNullValues());


        Page<CopilotOperation> pageInfo = copilotRepository.findAll(exampleObj, pageable);

        //判断是否存在下一页
        if (pageInfo.getTotalElements() - page.longValue() * limit > 0) {
            hasNext = true;
        }
        CopilotPageInfo copilotPageInfo = new CopilotPageInfo();
        copilotPageInfo.setPage(pageInfo.getTotalPages())
                .setTotal(pageInfo.getTotalElements())
                .setHasNext(hasNext)
                .setData(pageInfo.getContent());
        return MaaResult.success(copilotPageInfo);
    }


    public MaaResult<Void> update(CopilotOperation copilotOperation) {
        Boolean owner = verifyOwner(copilotOperation.getId());
        if (owner) {
            copilotRepository.save(copilotOperation);
            return MaaResult.success(null);
        } else {
            throw new MaaResultException("无法更新他人作业");
        }
    }

    public MaaResult<Void> rates(CopilotRequest request) {

        return MaaResult.success(null);
    }
}