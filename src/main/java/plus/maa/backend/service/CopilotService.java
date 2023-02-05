package plus.maa.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import plus.maa.backend.common.utils.converter.CopilotConverter;
import plus.maa.backend.controller.request.*;
import plus.maa.backend.controller.response.*;
import plus.maa.backend.repository.*;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.service.model.LoginUser;
import plus.maa.backend.service.model.RatingCache;
import plus.maa.backend.service.model.RatingType;

/**
 * @author LoMu
 *         Date 2022-12-25 19:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotService {
    private final CopilotRepository copilotRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper mapper;
    private final ArkLevelService levelService;

    private final HttpServletRequest request;

    private final RedisCache redisCache;

    private final TableLogicDelete tableLogicDelete;

    private final CopilotRatingRepository copilotRatingRepository;
    private final AtomicLong copilotId = new AtomicLong(20000);

    @PostConstruct
    public void init() {
        // 初始化copilotId, 从数据库中获取最大的copilotId
        // 如果数据库中没有数据, 则从20000开始
        copilotRepository.findFirstByOrderByCopilotIdDesc()
                .map(Copilot::getCopilotId)
                .ifPresent(last -> copilotId.set(last + 1));

        log.info("作业自增ID初始化完成: {}", copilotId.get());
    }

    /**
     * 根据_id获取Copilot
     *
     * @param id _id
     * @return Copilot
     */
    private Copilot findById(String id) {
        Copilot copilot;
        // 如果id为纯数字, 则使用copilotId查询
        if (StringUtils.isNumeric(id)) {
            copilot = copilotRepository.findByCopilotId(Long.parseLong(id)).orElse(null);
        } else {
            copilot = copilotRepository.findById(id).orElse(null);
        }

        if (copilot == null) {
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

        String userId = user.getMaaUser().getUserId();
        Copilot copilot = findById(operationId);
        return Objects.equals(copilot.getUploaderId(), userId);
    }

    /**
     * 验证数值是否合法
     * 并修正前端的冗余部分
     *
     * @param copilotDTO copilotDTO
     */
    private CopilotDTO CorrectCopilot(CopilotDTO copilotDTO) {

        // 去除name的冗余部分
        copilotDTO.getGroups().forEach(
                groups -> groups.getOpers().forEach(opers -> opers.setName(opers.getName().replaceAll("[\"“”]", ""))));
        copilotDTO.getOpers().forEach(operator -> operator.setName(operator.getName().replaceAll("[\"“”]", "")));

        // actions name 不是必须
        copilotDTO.getActions().forEach(action -> {
            if (action.getName() == null)
                return;
            action.setName(action.getName().replaceAll("[\"“”]", ""));
        });
        return copilotDTO;
    }

    /**
     * 将content解析为CopilotDTO
     *
     * @param content content
     * @return CopilotDTO
     */
    private CopilotDTO parseToCopilotDto(String content) {
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
    public MaaResult<Long> upload(LoginUser user, String content) {
        CopilotDTO copilotDTO = CorrectCopilot(parseToCopilotDto(content));
        Date date = new Date();
        // 将其转换为数据库存储对象
        Copilot copilot = CopilotConverter.INSTANCE.toCopilot(copilotDTO);
        // 设置copilotId
        copilot.setCopilotId(copilotId.getAndIncrement());

        copilot.setUploaderId(user.getMaaUser().getUserId())
                .setUploader(user.getMaaUser().getUserName())
                .setFirstUploadTime(date)
                .setUploadTime(date);

        copilotRepository.insert(copilot);
        copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId()));
        return MaaResult.success(copilot.getCopilotId());
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
            tableLogicDelete.deleteCopilotById(operationId);

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
    public MaaResult<CopilotInfo> getCopilotById(LoginUser user, String id) {
        String userId = getUserId(user);
        // 根据ID获取作业, 如作业不存在则抛出异常返回
        Copilot copilot = findById(id);
        // 如果使用数字ID查询, 则将其转换为MongoId
        final String saveId = StringUtils.isNumeric(id) ? copilot.getId() : id;
        if (Objects.isNull(saveId)) {
            throw new MaaResultException("作业数据异常");
        }
        // 60分钟内限制同一个用户对访问量的增加
        RatingCache cache = redisCache.getCache("views:" + userId, RatingCache.class);
        if (Objects.isNull(cache) || !cache.getCache().contains(saveId)) {
            Query query = Query.query(Criteria.where("id").is(saveId).and("delete").is(false));
            Update update = new Update();
            // 增加一次views
            update.inc("views");
            mongoTemplate.updateFirst(query, update, Copilot.class);
            if (Objects.isNull(cache)) {
                redisCache.setCache("views:" + userId, new RatingCache(List.of(saveId)));
            } else {
                redisCache.updateCache("views:" + userId, RatingCache.class, cache, updateCache -> {
                    updateCache.getCache().add(saveId);
                    return updateCache;
                }, 60, TimeUnit.MINUTES);
            }
        }
        CopilotInfo info = formatCopilot(userId, copilot);
        return MaaResult.success(info);
    }

    /**
     * 分页查询
     *
     * @param user    获取已登录用户自己的作业数据
     * @param request 模糊查询
     * @return CopilotPageInfo
     */

    // 如果是查最新数据或指定搜索 就不缓存
    @Cacheable(value = "copilotPage", condition = "#request.levelKeyword != null && ''.equals(#request.levelKeyword) " +
            "|| #request.operator != null && ''.equals(#request.operator)" +
            "|| #request.orderBy != null && ('hot'.equals(#request.orderBy) || 'views'.equals(#request.orderBy)) " +
            "|| #request.uploaderId != null && ''.equals(#request.uploaderId)")
    public MaaResult<CopilotPageInfo> queriesCopilot(LoginUser user, CopilotQueriesRequest request) {
        String userId = getUserId(user);
        Sort.Order sortOrder = new Sort.Order(
                request.isDesc() ? Sort.Direction.DESC : Sort.Direction.ASC,
                Optional.ofNullable(request.getOrderBy())
                        .filter(StringUtils::isNotBlank)
                        .map(ob -> Objects.equals(request.getOrderBy(), "hot") ? "hotScore" : request.getOrderBy())
                        .orElse("copilotId"));
        int page = 1;
        int limit = 10;
        boolean hasNext = false;

        // 判断是否有值 无值则为默认
        if (request.getPage() > 0) {
            page = request.getPage();
        }
        if (request.getLimit() > 0) {
            limit = request.getLimit();
        }

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortOrder));

        // 模糊查询
        Query queryObj = new Query();
        Criteria criteriaObj = new Criteria();
        Set<Criteria> andQueries = new HashSet<>();
        Set<Criteria> norQueries = new HashSet<>();
        Set<Criteria> orQueries = new HashSet<>();
        andQueries.add(Criteria.where("delete").is(false));

        // 匹配模糊查询
        if (StringUtils.isNotBlank(request.getLevelKeyword())) {
            ArkLevelInfo levelInfo = levelService.queryLevel(request.getLevelKeyword());
            if (levelInfo != null) {
                andQueries.add(Criteria.where("stageName").regex(levelInfo.getStageId()));
            }
        }
        // or模糊查询
        if (StringUtils.isNotBlank(request.getDocument())) {
            orQueries.add(Criteria.where("doc.title").regex(request.getDocument()));
            orQueries.add(Criteria.where("doc.details").regex(request.getDocument()));
        }

        // operator 包含或排除干员查询
        // 排除~开头的 查询非~开头
        String oper = request.getOperator();
        if (!ObjectUtils.isEmpty(oper)) {
            oper = oper.replaceAll("[“\"”]", "");
            String[] operators = oper.split(",");
            for (String operator : operators) {
                if (operator.startsWith("~")) {
                    String exclude = operator.substring(1);
                    // 排除查询指定干员
                    norQueries.add(Criteria.where("opers.name").regex(exclude));
                } else {
                    // 模糊匹配查询指定干员
                    andQueries.add(Criteria.where("opers.name").regex(operator));
                }
            }
        }

        // 匹配查询
        if (StringUtils.isNotBlank(request.getUploaderId()) && "me".equals(request.getUploaderId())) {
            String Id = user.getMaaUser().getUserId();
            if (!ObjectUtils.isEmpty(Id))
                andQueries.add(Criteria.where("uploader").is(Id));
        }

        // 封装查询
        if (andQueries.size() > 0)
            criteriaObj.andOperator(andQueries);
        if (norQueries.size() > 0)
            criteriaObj.norOperator(norQueries);
        if (orQueries.size() > 0)
            criteriaObj.orOperator(orQueries);
        queryObj.addCriteria(criteriaObj);

        // 查询总数
        long count = mongoTemplate.count(queryObj, Copilot.class);

        // 分页排序查询
        List<Copilot> copilots = mongoTemplate.find(queryObj.with(pageable), Copilot.class);
        // 填充前端所需信息
        List<CopilotInfo> infos = copilots.stream().map(copilot -> formatCopilot(userId, copilot)).toList();

        // 计算页面
        int pageNumber = (int) Math.ceil((double) count / limit);

        // 判断是否存在下一页
        if (count - (long) page * limit > 0) {
            hasNext = true;
        }

        // 封装数据
        CopilotPageInfo copilotPageInfo = new CopilotPageInfo();
        copilotPageInfo.setTotal(count)
                .setHasNext(hasNext)
                .setData(infos)
                .setPage(pageNumber);
        return MaaResult.success(copilotPageInfo);
    }

    /**
     * 增量更新
     *
     * @param copilotCUDRequest 作业_id content
     * @return null
     */
    public MaaResult<Void> update(LoginUser loginUser, CopilotCUDRequest copilotCUDRequest) {
        String content = copilotCUDRequest.getContent();
        String id = copilotCUDRequest.getId();
        CopilotDTO copilotDTO = CorrectCopilot(parseToCopilotDto(content));
        Boolean owner = verifyOwner(loginUser, id);

        if (owner) {
            Copilot rawCopilot = findById(id);
            rawCopilot.setUploadTime(new Date());
            CopilotConverter.INSTANCE.updateCopilotFromDto(copilotDTO, rawCopilot);
            copilotRepository.save(rawCopilot);
            return MaaResult.success(null);
        } else {
            throw new MaaResultException("无法更新他人作业");
        }
    }

    /**
     * 评分相关
     *
     * @param request   评分
     * @param loginUser 用于已登录用户作出评分
     * @return null
     */
    public MaaResult<String> rates(LoginUser loginUser, CopilotRatingReq request) {
        String userId = getUserId(loginUser);
        String rating = request.getRating();
        // 获取评分表
        Query query = Query.query(Criteria.where("copilotId").is(request.getId()));
        Update update = new Update();

        // 查询指定作业评分
        CopilotRating copilotRating = mongoTemplate.findOne(query, CopilotRating.class);
        // 如果是早期创建的作业表可能无法做出评分
        if (copilotRating == null)
            throw new MaaResultException("server error: Rating is null");

        boolean existUserId = false;
        // 点赞数
        int likeCount = 0;
        List<CopilotRating.RatingUser> ratingUsers = copilotRating.getRatingUsers();

        // 查看是否已评分 如果已评分则进行更新 如果做出相同的评分则直接返回
        for (CopilotRating.RatingUser ratingUser : ratingUsers) {
            if (userId.equals(ratingUser.getUserId())) {
                if (ratingUser.getRating().equals(rating))
                    return MaaResult.success("评分成功");
                existUserId = true;
                ratingUser.setRating(rating);

            }
            if ("Like".equals(ratingUser.getRating()))
                likeCount++;
        }
        // 如果新添加的评分是like
        if ("Like".equals(rating))
            likeCount++;

        // 不存在评分 则添加新的评分
        CopilotRating.RatingUser ratingUser;
        if (!existUserId) {
            ratingUser = new CopilotRating.RatingUser(userId, rating);
            ratingUsers.add(ratingUser);
            update.addToSet("ratingUsers", ratingUser);
            mongoTemplate.updateFirst(query, update, CopilotRating.class);
        }

        // 计算评分相关
        int ratingCount = copilotRating.getRatingUsers().size();
        double rawRatingLevel = (double) likeCount / ratingCount;
        BigDecimal bigDecimal = new BigDecimal(rawRatingLevel);
        // 只取一位小数点
        double ratingLevel = bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();

        // 更新数据
        copilotRating.setRatingUsers(ratingUsers);
        copilotRating.setRatingLevel((int) (ratingLevel * 10));
        copilotRating.setRatingRatio(ratingLevel);
        mongoTemplate.save(copilotRating);
        // TODO 计算热度 (暂时)
        double hotScore = rawRatingLevel + likeCount;
        // 更新热度
        copilotRepository.findByCopilotId(request.getId()).ifPresent(copilot -> {
            copilot.setHotScore(hotScore);
            mongoTemplate.save(copilot);
        });

        return MaaResult.success("评分成功");
    }

    /**
     * 重构当前已存在的数据库<br/>
     * 生成评分表..
     *
     * @return null
     */
    public MaaResult<Void> refactorExistingDatabase() {
        List<Copilot> all = copilotRepository.findAll();
        List<Copilot> notExistRatingTable = all.stream()
                .filter(a -> !copilotRatingRepository.existsCopilotRatingByCopilotId(a.getId())).toList();
        notExistRatingTable
                .forEach(copilot -> copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId())));
        // 转换数据存储类型
        copilotRepository.saveAll(all);
        return MaaResult.success(null);
    }

    /**
     * 将数据库内容转换为前端所需格式<br>
     * TODO 当前仅为简单转换，具体细节待定
     */
    private CopilotInfo formatCopilot(String userId, Copilot copilot) {
        CopilotInfo info = CopilotConverter.INSTANCE.toCopilotInfo(copilot);
        // 设置干员信息
        List<String> operStrList = copilot.getOpers().stream()
                .map(o -> String.format("%s::%s", o.getName(), o.getSill()))
                .toList();

        // 设置干员组干员信息
        if (copilot.getGroups() != null) {
            List<String> operators = new ArrayList<>();
            for (Copilot.Groups group : copilot.getGroups()) {
                if (group.getOpers() != null) {
                    for (Copilot.OperationGroup oper : group.getOpers()) {
                        String format = String.format("%s::%s", oper.getName(), oper.getSill());
                        operators.add(format);
                    }
                }
                group.setOperators(operators);
            }
        }

        info.setOpers(operStrList);
        info.setOperators(operStrList);

        ArkLevelInfo levelInfo = levelService.findByLevelId(copilot.getStageName());
        Optional<CopilotRating> copilotRating = copilotRatingRepository.findByCopilotId(copilot.getCopilotId());

        // 判断评分中是否有当前用户评分记录 有则获取其评分并将其转换为 0 = None 1 = LIKE 2 = DISLIKE
        copilotRating.map(cr -> {
            info.setRatingRatio(cr.getRatingRatio());
            info.setRatingLevel(cr.getRatingLevel());
            return cr.getRatingUsers();
        }).ifPresent(rus -> {
            // 评分数少于一定数量
            info.setNotEnoughRating(rus.size() > 5);
            rus.stream()
                    .filter(ru -> Objects.equals(userId, ru.getUserId()))
                    .findFirst()
                    .ifPresent(fst -> {
                        int rating = RatingType.fromRatingType(fst.getRating()).getDisplay();
                        info.setRatingType(rating);
                    });
        });

        info.setLevel(levelInfo);
        info.setAvailable(true);

        if (copilot.getCopilotId() != null) {
            try {
                // 兼容客户端, 将作业ID替换为数字ID
                copilot.setId(Long.toString(copilot.getCopilotId()));
                info.setContent(mapper.writeValueAsString(copilot));
            } catch (JsonProcessingException e) {
                log.error("json序列化失败", e);
            }
        }
        return info;
    }

    /**
     * 获取用户唯一标识符<br/>
     * 如果未登录获取ip <br/>
     * 如果已登录获取id
     *
     * @param loginUser LoginUser
     * @return 用户标识符
     */
    private String getUserId(LoginUser loginUser) {
        // TODO 此处更换为调用IPUtil工具类
        String id = request.getRemoteAddr();
        if (request.getHeader("x-forwarded-for") != null) {
            id = request.getHeader("x-forwarded-for");
        }
        // 账户已登录? 获取userId
        if (!ObjectUtils.isEmpty(loginUser)) {
            id = loginUser.getMaaUser().getUserId();
        }
        return id;
    }
}