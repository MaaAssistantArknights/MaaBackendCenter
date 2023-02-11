package plus.maa.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.common.utils.IpUtil;
import plus.maa.backend.common.utils.converter.CopilotConverter;
import plus.maa.backend.controller.request.CopilotCUDRequest;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.request.CopilotQueriesRequest;
import plus.maa.backend.controller.request.CopilotRatingReq;
import plus.maa.backend.controller.response.*;
import plus.maa.backend.repository.CopilotRatingRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.TableLogicDelete;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.service.model.LoginUser;
import plus.maa.backend.service.model.RatingCache;
import plus.maa.backend.service.model.RatingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author LoMu
 * Date 2022-12-25 19:57
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
    private final AtomicLong copilotIncrementId = new AtomicLong(20000);

    @PostConstruct
    public void init() {
        // 初始化copilotId, 从数据库中获取最大的copilotId
        // 如果数据库中没有数据, 则从20000开始
        copilotRepository.findFirstByOrderByCopilotIdDesc()
                .map(Copilot::getCopilotId)
                .ifPresent(last -> copilotIncrementId.set(last + 1));

        log.info("作业自增ID初始化完成: {}", copilotIncrementId.get());
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

        Assert.notNull(copilot, "作业不存在");
        return copilot;
    }

    /**
     * 验证当前账户是否为作业创建者
     *
     * @param operationId 作业id
     */
    private void verifyOwner(LoginUser user, String operationId) {
        Assert.hasText(operationId, "作业id不可为空");
        String userId = user.getMaaUser().getUserId();
        Copilot copilot = findById(operationId);
        Assert.state(Objects.equals(copilot.getUploaderId(), userId), "您无法修改不属于您的作业");
    }

    /**
     * 验证数值是否合法
     * 并修正前端的冗余部分
     *
     * @param copilotDTO copilotDTO
     */
    private CopilotDTO correctCopilot(CopilotDTO copilotDTO) {

        // 去除name的冗余部分
        // todo 优化空处理代码美观程度
        if (copilotDTO.getGroups() != null) {
            copilotDTO.getGroups().forEach(
                    group -> {
                        if (group.getOpers() != null) {
                            group.getOpers().forEach(oper -> oper.setName(oper.getName() == null ?
                                    null : oper.getName().replaceAll("[\"“”]", "")));
                        }
                    }
            );
        }
        if (copilotDTO.getOpers() != null) {
            copilotDTO.getOpers().forEach(operator ->
                    operator.setName(operator.getName() == null ?
                            null : operator.getName().replaceAll("[\"“”]", "")));
        }

        // actions name 不是必须
        if (copilotDTO.getActions() != null) {
            copilotDTO.getActions().forEach(action -> action.setName(action.getName() == null ?
                    null : action.getName().replaceAll("[\"“”]", "")));
        }
        // 使用stageId存储作业关卡信息
        ArkLevelInfo level = levelService.findByLevelIdFuzzy(copilotDTO.getStageName());
        if (level != null) {
            copilotDTO.setStageName(level.getStageId());
        }
        return copilotDTO;
    }

    /**
     * 将content解析为CopilotDTO
     *
     * @param content content
     * @return CopilotDTO
     */
    private CopilotDTO parseToCopilotDto(String content) {
        Assert.notNull(content, "作业内容不可为空");
        try {
            return mapper.readValue(content, CopilotDTO.class);
        } catch (JsonProcessingException e) {
            log.error("解析copilot失败", e);
            throw new MaaResultException("解析copilot失败");
        }
    }

    /**
     * 上传新的作业
     *
     * @param content 前端编辑json作业内容
     * @return 返回_id
     */
    public MaaResult<Long> upload(LoginUser user, String content) {
        CopilotDTO copilotDTO = correctCopilot(parseToCopilotDto(content));
        // 将其转换为数据库存储对象
        Copilot copilot = CopilotConverter.INSTANCE.toCopilot(
                copilotDTO, user.getMaaUser(),
                new Date(), copilotIncrementId.getAndIncrement(),
                content);
        copilotRepository.insert(copilot);
        copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId()));
        return MaaResult.success(copilot.getCopilotId());
    }

    /**
     * 根据作业id删除作业
     */
    public MaaResult<Void> delete(LoginUser user, CopilotCUDRequest request) {
        String operationId = request.getId();
        verifyOwner(user, operationId);
        tableLogicDelete.deleteCopilotById(operationId);
        return MaaResult.success();
    }

    /**
     * 指定查询
     */
    public MaaResult<CopilotInfo> getCopilotById(LoginUser user, Long id) {
        String userId = getUserId(user);
        // 根据ID获取作业, 如作业不存在则抛出异常返回
        Optional<Copilot> copilotOptional = copilotRepository.findByCopilotId(id);
        return MaaResult.success(copilotOptional.map(copilot -> {
            // 60分钟内限制同一个用户对访问量的增加
            RatingCache cache = redisCache.getCache("views:" + userId, RatingCache.class);
            if (Objects.isNull(cache) || Objects.isNull(cache.getCopilotIds()) ||
                    !cache.getCopilotIds().contains(id)) {
                Query query = Query.query(Criteria.where("copilotId").is(id).and("delete").is(false));
                Update update = new Update();
                // 增加一次views
                update.inc("views");
                mongoTemplate.updateFirst(query, update, Copilot.class);
                if (Objects.isNull(cache)) {
                    redisCache.setCache("views:" + userId, new RatingCache(Sets.newHashSet(id)));
                } else {
                    redisCache.updateCache("views:" + userId, RatingCache.class, cache,
                            updateCache -> {
                                updateCache.getCopilotIds().add(id);
                                return updateCache;
                            }, 60, TimeUnit.MINUTES);
                }
            }
            CopilotRating rating = copilotRatingRepository.findByCopilotId(copilot.getCopilotId());
            return formatCopilot(userId, copilot, rating);
        }).orElse(null));
    }

    /**
     * 分页查询
     *
     * @param user    获取已登录用户自己的作业数据
     * @param request 模糊查询
     * @return CopilotPageInfo
     */
    public MaaResult<CopilotPageInfo> queriesCopilot(LoginUser user, CopilotQueriesRequest request) {
        String userId = getUserId(user);
        Sort.Order sortOrder = new Sort.Order(
                request.isDesc() ? Sort.Direction.DESC : Sort.Direction.ASC,
                Optional.ofNullable(request.getOrderBy())
                        .filter(StringUtils::isNotBlank)
                        .map(ob -> switch (ob) {
                            case "hot" -> "hotScore";
                            case "id" -> "copilotId";
                            default -> request.getOrderBy();
                        }).orElse("copilotId"));
        // 判断是否有值 无值则为默认
        int page = request.getPage() > 0 ? request.getPage() : 1;
        int limit = request.getLimit() > 0 ? request.getLimit() : 10;
        boolean hasNext = false;

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
            ArkLevelInfo levelInfo = levelService.queryLevelByKeyword(request.getLevelKeyword());
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
        if (StringUtils.isNotBlank(oper)) {
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
        if (StringUtils.isNotBlank(request.getUploaderId())) {
            if ("me".equals(request.getUploaderId())) {
                String loginUserId = user.getMaaUser().getUserId();
                if (!ObjectUtils.isEmpty(loginUserId)) {
                    andQueries.add(Criteria.where("uploaderId").is(loginUserId));
                }
            } else {
                andQueries.add(Criteria.where("uploaderId").is(request.getUploaderId()));
            }
        }

        // 封装查询
        if (andQueries.size() > 0) {
            criteriaObj.andOperator(andQueries);
        }
        if (norQueries.size() > 0) {
            criteriaObj.norOperator(norQueries);
        }
        if (orQueries.size() > 0) {
            criteriaObj.orOperator(orQueries);
        }
        queryObj.addCriteria(criteriaObj);

        // 查询总数
        long count = mongoTemplate.count(queryObj, Copilot.class);

        // 分页排序查询
        List<Copilot> copilots = mongoTemplate.find(queryObj.with(pageable), Copilot.class);
        // 填充前端所需信息
        Set<Long> copilotIds = copilots.stream().map(Copilot::getCopilotId).collect(Collectors.toSet());
        List<CopilotRating> ratings = copilotRatingRepository.findByCopilotIdIn(copilotIds);
        Map<Long, CopilotRating> ratingByCopilotId = Maps.uniqueIndex(ratings, CopilotRating::getCopilotId);
        List<CopilotInfo> infos = copilots.stream().map(copilot ->
                        formatCopilot(userId, copilot,
                                ratingByCopilotId.get(copilot.getCopilotId())))
                .toList();
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
        CopilotDTO copilotDTO = correctCopilot(parseToCopilotDto(content));
        verifyOwner(loginUser, id);
        Copilot rawCopilot = findById(id);
        rawCopilot.setUploadTime(new Date());
        CopilotConverter.INSTANCE.updateCopilotFromDto(copilotDTO, content, rawCopilot);
        copilotRepository.save(rawCopilot);
        return MaaResult.success(null);
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
        Assert.notNull(copilotRating, "Rating is null");

        boolean existUserId = false;
        // 点赞数
        int likeCount = 0;
        int disLikeCount = 0;
        List<CopilotRating.RatingUser> ratingUsers = copilotRating.getRatingUsers();

        // 查看是否已评分 如果已评分则进行更新 如果做出相同的评分则直接返回
        for (CopilotRating.RatingUser ratingUser : ratingUsers) {
            if (userId.equals(ratingUser.getUserId())) {
                if (ratingUser.getRating().equals(rating)) {
                    return MaaResult.success("评分成功");
                }
                existUserId = true;
                ratingUser.setRating(rating);
            }
            if ("Like".equals(ratingUser.getRating())) {
                likeCount++;
            }
            if ("Dislike".equals(ratingUser.getRating())) {
                disLikeCount++;
            }
        }
        // 如果新添加的评分是like
        if ("Like".equals(rating)) {
            likeCount++;
        }
        if ("Dislike".equals(rating)) {
            disLikeCount++;
        }

        // 不存在评分 则添加新的评分
        if (!existUserId) {
            CopilotRating.RatingUser ratingUser = new CopilotRating.RatingUser(userId, rating);
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
        double hotScore = rawRatingLevel + (likeCount - disLikeCount);
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
                .filter(a -> !copilotRatingRepository.existsCopilotRatingByCopilotId(a.getCopilotId())).toList();
        notExistRatingTable
                .forEach(copilot -> copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId())));
        // 转换数据存储类型
        copilotRepository.saveAll(all);
        return MaaResult.success(null);
    }

    /**
     * 将数据库内容转换为前端所需格式<br>
     */
    private CopilotInfo formatCopilot(String userId, Copilot copilot, CopilotRating rating) {
        CopilotInfo info = CopilotConverter.INSTANCE.toCopilotInfo(copilot);


        Optional<CopilotRating> copilotRating = Optional.ofNullable(rating);

        // 判断评分中是否有当前用户评分记录 有则获取其评分并将其转换为 0 = None 1 = LIKE 2 = DISLIKE
        copilotRating.map(cr -> {
            info.setRatingRatio(cr.getRatingRatio());
            info.setRatingLevel(cr.getRatingLevel());
            return cr.getRatingUsers();
        }).ifPresent(rus -> {
            // 评分数少于一定数量
            info.setNotEnoughRating(rus.size() <= 5);
            rus.stream()
                    .filter(ru -> Objects.equals(userId, ru.getUserId()))
                    .findFirst()
                    .ifPresent(fst ->
                            info.setRatingType(RatingType.fromRatingType(fst.getRating()).getDisplay())
                    );
        });

        info.setAvailable(true);

        try {
            // 兼容客户端, 将作业ID替换为数字ID
            copilot.setId(Long.toString(copilot.getCopilotId()));
            if (StringUtils.isEmpty(info.getContent())) {
                // 设置干员组干员信息
                if (copilot.getGroups() != null) {
                    List<String> operators = new ArrayList<>();
                    for (Copilot.Groups group : copilot.getGroups()) {
                        if (group.getOpers() != null) {
                            for (Copilot.OperationGroup oper : group.getOpers()) {
                                String format = String.format("%s::%s", oper.getName(), oper.getSkill());
                                operators.add(format);
                            }
                        }
                        group.setOperators(operators);
                    }
                }
                String content = mapper.writeValueAsString(copilot);
                info.setContent(content);
                updateContent(copilot.getCopilotId(), content);
            }
        } catch (JsonProcessingException e) {
            log.error("json序列化失败", e);
        }
        return info;
    }

    private void updateContent(Long copilotId, String content) {
        copilotRepository.findByCopilotId(copilotId).ifPresent(c -> {
            c.setContent(content);
            copilotRepository.save(c);
        });
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
        String id;
        if (!ObjectUtils.isEmpty(loginUser)) {
            id = loginUser.getMaaUser().getUserId();
        } else {
            id = IpUtil.getIpAddr(request);
        }
        return id;
    }
}
