package plus.maa.backend.service;


import cn.hutool.core.lang.ObjectId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.controller.request.CopilotCUDRequest;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.request.CopilotQueriesRequest;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotMapper;
import plus.maa.backend.service.model.LoginUser;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author LoMu
 * Date  2022-12-25 19:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotService {
    private final CopilotRepository copilotRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper mapper;

    private final CopilotMapper copilotMapper;

    /**
     * 根据_id获取Copilot
     *
     * @param id _id
     * @return Copilot
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
     * 验证当前账户是否为作业创建者
     *
     * @param operationId 作业id
     * @return boolean
     */
    private Boolean verifyOwner(LoginUser user, String operationId) {
        if (operationId == null) {
            throw new MaaResultException("作业id不可为空");
        }

        /*String userId = user.getMaaUser().getUserId();*/
        String userId = "66666";
        Copilot copilot = findByid(operationId);
        return Objects.equals(copilot.getUploaderId(), userId);
    }

    /**
     * 验证数值是否合法
     *
     * @param copilotDto copilot
     */
    private void verifyCopilot(CopilotDTO copilotDto) {

        if (copilotDto.getActions() != null) {
            for (Copilot.Action action : copilotDto.getActions()) {
                String type = action.getType();

                if ("SkillUsage".equals(type) || "技能用法".equals(type)) {
                    if (action.getSkillUsage() == null) {
                        throw new MaaResultException("当动作类型为技能用法时,技能用法该选项必选");
                    }
                }

                if (action.getLocation() != null) {
                    if (action.getLocation().length > 2) {
                        throw new MaaResultException("干员位置的数据格式不符合规定");
                    }
                }

            }
        }

    }


    private CopilotDTO contentToCopilotDto(String content) {
        if (content == null) {
            throw new MaaResultException("数据不可为空");
        }
        CopilotDTO copilotDto;
        try {
            copilotDto = mapper.readValue(content, CopilotDTO.class);
        } catch (JsonProcessingException e) {
            log.error("解析copilot失败", e);
            throw new MaaResultException("解析copilot失败");
        }
        return copilotDto;
    }

    /**
     * 上传新的作业
     *
     * @param content 前端编辑json作业内容
     * @return 返回_id
     */
    public MaaResult<String> upload(LoginUser user, String content) {
        CopilotDTO copilotDto = contentToCopilotDto(content);
        String id = ObjectId.next();
        Date date = new Date();

        verifyCopilot(copilotDto);


        Copilot copilot = copilotMapper.toCopilot(copilotDto);
        copilot.setUploaderId(user.getMaaUser().getUserId())
                .setUploader(user.getMaaUser().getUserName())
                .setCreateDate(date)
                .setUpdateDate(date)
                .setId(id);

        try {
            copilotRepository.insert(copilot);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return MaaResult.success(id);
    }

    /**
     * 删除指定_id
     *
     * @param request _id
     * @return null
     */
    public MaaResult<Void> delete(LoginUser user, CopilotCUDRequest request) {
        String operationId = request.getId();

        if (verifyOwner(user, operationId)) {
            copilotRepository.deleteById(operationId);
            return MaaResult.success(null);
        } else {
            throw new MaaResultException("无法删除他人作业");
        }

    }

    /**
     * 指定查询
     *
     * @param id copilot _id
     * @return copilotInfo
     */
    public MaaResult<Copilot> getCopilotById(String id) {
        //增加一次views
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
     * @param request 模糊查询
     * @return CopilotPageInfo
     */
    public MaaResult<CopilotPageInfo> queriesCopilot(CopilotQueriesRequest request) {
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
        if (request.getOrderBy() != null && !"".equals(request.getOrderBy())) {
            orderby = request.getOrderBy();
        }
        if (request.getDesc() != null && request.getDesc()) {
            sortOrder = new Sort.Order(Sort.Direction.DESC, orderby);
        }

        Pageable pageable = PageRequest.of(
                page - 1, limit
                , Sort.by(sortOrder));


        //模糊查询
        Query queryObj = new Query();
        Criteria criteriaObj = new Criteria();

        //匹配模糊查询
        if (request.getLevelKeyword() != null && !"".equals(request.getLevelKeyword())) {
            criteriaObj.and("stageName").regex(request.getLevelKeyword());
        }
        //or模糊查询
        if (request.getDocument() != null && !"".equals(request.getDocument())) {
            criteriaObj.orOperator(
                    Criteria.where("doc.title").regex(request.getDocument()),
                    Criteria.where("doc.details").regex(request.getDocument())
            );
        }

        //operator 包含或排除干员查询
        //排除~开头的 查询非~开头
        String oper = request.getOperator();
        if (!ObjectUtils.isEmpty(oper)) {
            String[] operators = oper.split(",");
            for (String operator : operators) {
                if ("~".equals(operator.substring(0, 1))) {
                    String exclude = operator.substring(1);
                    //排除查询指定干员
                    criteriaObj.norOperator(
                            Criteria.where("opers.name").regex(exclude),
                            Criteria.where("opers.name").regex(exclude));
                } else {
                    //模糊匹配查询指定干员
                    criteriaObj.and("opers.name").regex(operator);
                }
            }
        }

        //匹配查询
        if (request.getUploader() != null && !"".equals(request.getUploader())) {
            criteriaObj.and("uploader").is(request.getUploader());
        }

        //封装查询
        queryObj.addCriteria(criteriaObj);

        //查询总数
        long count = mongoTemplate.count(queryObj, Copilot.class);

        //分页排序查询
        List<Copilot> copilots = mongoTemplate.find(queryObj.with(pageable), Copilot.class);

        //计算页面
        int pageNumber = (int) Math.ceil((double) count / limit);

        //判断是否存在下一页
        if (count - (long) page * limit > 0) {
            hasNext = true;
        }

        //封装数据
        CopilotPageInfo copilotPageInfo = new CopilotPageInfo();
        copilotPageInfo.setTotal(count)
                .setHasNext(hasNext)
                .setData(copilots)
                .setPage(pageNumber);
        return MaaResult.success(copilotPageInfo);
    }

    /**
     * 增量更新
     *
     * @param id      作业_id
     * @param content json
     * @return null
     */
    public MaaResult<Void> update(LoginUser loginUser, String id, String content) {
        CopilotDTO copilotDto = contentToCopilotDto(content);
        Boolean owner = verifyOwner(loginUser, id);
        verifyCopilot(copilotDto);
        if (owner) {
            Copilot rawCopilot = findByid(id);
            rawCopilot.setUpdateDate(new Date());
            copilotMapper.updateCopilotToRaw(copilotDto, rawCopilot);
            copilotRepository.save(rawCopilot);
            return MaaResult.success(null);
        } else {
            throw new MaaResultException("无法更新他人作业");
        }
    }


    /**
     * 评分相关
     *
     * @param request 评分
     * @return null
     */
    public MaaResult<Void> rates(CopilotQueriesRequest request) {
        // TODO: 评分相关
        return MaaResult.success(null);
    }
}