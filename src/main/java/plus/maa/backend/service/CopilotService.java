package plus.maa.backend.service;

import cn.hutool.core.lang.ObjectId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import plus.maa.backend.controller.request.CopilotRequest;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.LoginUser;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author LoMu
 * Date  2022-12-25 19:57
 */
@Service
@RequiredArgsConstructor
public class CopilotService {

    private final CopilotRepository copilotRepository;
    private final MongoTemplate mongoTemplate;


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
    private Copilot findByid(String id) {
        Optional<Copilot> optional = copilotRepository.findById(id);

        Copilot copilot;
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
        Copilot copilot = findByid(operationId);
        return Objects.equals(copilot.getUploaderId(), userId);
    }


    public MaaResult<String> upload(Copilot copilot) {
        LoginUser user = getCurrentUser();
        String id = ObjectId.next();
        copilot
                .setUploaderId(user.getMaaUser().getUserId())
                .setUploader(user.getMaaUser().getUserName())
                /* .setCreateDate(LocalDateTime.now())
                 .setUpdateDate(LocalDateTime.now())*/
                .setId(id);

        try {
            copilotRepository.insert(copilot);
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


    public MaaResult<Copilot> getCoplilotById(String id) {
        Copilot copilot = findByid(id);
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update();
        update.inc("views");
        mongoTemplate.updateFirst(query, update, Copilot.class);
        return MaaResult.success(copilot);
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
        int page = 1;
        int limit = 10;
        boolean hasNext = false;

        //判断是否有值 无值则为默认
        if (request.getPage() != null && request.getPage() > 0) {
            page = request.getPage();
        }
        if (request.getLimit() != null && request.getLimit() > 0) {
            limit = request.getLimit();
        }
        if (request.getOrderby() == null && !"".equals(request.getOrderby())) {
            orderby = request.getOrderby();
        }
        if (request.getDesc() != null) {
            sortOrder = new Sort.Order(Sort.Direction.DESC, orderby);
        }

        Pageable pageable = PageRequest.of(
                page - 1, limit
                , Sort.by(sortOrder));


        //模糊查询
        Query queryObj = new Query();
        Criteria criteriaObj = new Criteria();

        //or查询
        criteriaObj.orOperator(
                Criteria.where("doc.title").regex(request.getDocument()),
                Criteria.where("doc.details").regex(request.getDocument()),
                Criteria.where("arklevel.catOne").regex(request.getLevelKeyword()),
                Criteria.where("arklevel.catTne").regex(request.getLevelKeyword()),
                Criteria.where("arklevel.catThree").regex(request.getLevelKeyword()),
                Criteria.where("arklevel.name").regex(request.getLevelKeyword())
        );


        // operator 包含或排除干员
        String operator = request.getOperator();
        if (!"".equals(operator)) {
            String[] split = operator.split(",");
            for (String s : split) {
                if ("~".equals(s.substring(0, 1))) {
                    String exclude = s.substring(1);
                    //排除查询
                    criteriaObj.norOperator(
                            Criteria.where("actions.name").regex(exclude));
                } else {
                    //包含查询
                    criteriaObj.and("actions.name").is(s);
                }
            }
        }

        //is 查询
        if (!"".equals(request.getUploader())) {
            criteriaObj.and("uploader").is(request.getUploader());
        }

        queryObj.addCriteria(criteriaObj);
        //查询总数
        long count = mongoTemplate.count(queryObj, Copilot.class);
        List<Copilot> copilots = mongoTemplate.find(queryObj.with(pageable), Copilot.class);

        //计算页面
        int pageNumber = (int) Math.ceil((double) count / limit);

        //判断是否存在下一页
        if (count - (long) page * limit > 0) {
            hasNext = true;
        }
        CopilotPageInfo copilotPageInfo = new CopilotPageInfo();
        copilotPageInfo
                .setTotal(count)
                .setHasNext(hasNext)
                .setData(copilots)
                .setPage(pageNumber)
        ;
        return MaaResult.success(copilotPageInfo);
    }


    public MaaResult<Void> update(Copilot copilot) {
        Boolean owner = verifyOwner(copilot.getId());
        if (owner) {
            copilotRepository.save(copilot);
            return MaaResult.success(null);
        } else {
            throw new MaaResultException("无法更新他人作业");
        }
    }

    public MaaResult<Void> rates(CopilotRequest request) {

        return MaaResult.success(null);
    }
}