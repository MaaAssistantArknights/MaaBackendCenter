package plus.maa.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
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
import plus.maa.backend.common.utils.converter.CopilotConverter;
import plus.maa.backend.controller.request.CopilotCUDRequest;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.request.CopilotQueriesRequest;
import plus.maa.backend.controller.request.CopilotRatingReq;
import plus.maa.backend.controller.response.ArkLevelInfo;
import plus.maa.backend.controller.response.CopilotInfo;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.*;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.RatingCache;
import plus.maa.backend.service.model.RatingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
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
    private final RedisCache redisCache;
    private final UserRepository userRepository;
    private final CopilotRatingRepository copilotRatingRepository;
    private final AtomicLong copilotIncrementId = new AtomicLong(20000);
    private final CommentsAreaRepository commentsAreaRepository;

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
                            group.getOpers().forEach(oper -> oper
                                    .setName(oper.getName() == null ? null : oper.getName().replaceAll("[\"“”]", "")));
                        }
                    });
        }
        if (copilotDTO.getOpers() != null) {
            copilotDTO.getOpers().forEach(operator -> operator
                    .setName(operator.getName() == null ? null : operator.getName().replaceAll("[\"“”]", "")));
        }

        // actions name 不是必须
        if (copilotDTO.getActions() != null) {
            copilotDTO.getActions().forEach(action -> action
                    .setName(action.getName() == null ? null : action.getName().replaceAll("[\"“”]", "")));
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


    private Pattern caseInsensitive(String s) {
        return Pattern.compile(s, Pattern.CASE_INSENSITIVE);
    }


    /**
     * 上传新的作业
     *
     * @param content 前端编辑json作业内容
     * @return 返回_id
     */
    public Long upload(String loginUserId, String content) {
        CopilotDTO copilotDTO = correctCopilot(parseToCopilotDto(content));
        // 将其转换为数据库存储对象
        Copilot copilot = CopilotConverter.INSTANCE.toCopilot(
                copilotDTO, loginUserId,
                new Date(), copilotIncrementId.getAndIncrement(),
                content);
        copilotRepository.insert(copilot);
        copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId()));
        return copilot.getCopilotId();
    }

    /**
     * 根据作业id删除作业
     */
    public void delete(String loginUserId, CopilotCUDRequest request) {
        copilotRepository.findByCopilotId(request.getId()).ifPresent(copilot -> {
            Assert.state(Objects.equals(copilot.getUploaderId(), loginUserId), "您无法修改不属于您的作业");
            copilot.setDelete(true);
            copilotRepository.save(copilot);
        });
    }

    /**
     * 指定查询
     */
    public Optional<CopilotInfo> getCopilotById(String userIdOrIpAddress, Long id) {
        // 根据ID获取作业, 如作业不存在则抛出异常返回
        Optional<Copilot> copilotOptional = copilotRepository.findByCopilotIdAndDeleteIsFalse(id);
        return copilotOptional.map(copilot -> {
            // 60分钟内限制同一个用户对访问量的增加
            RatingCache cache = redisCache.getCache("views:" + userIdOrIpAddress, RatingCache.class);
            if (Objects.isNull(cache) || Objects.isNull(cache.getCopilotIds()) ||
                    !cache.getCopilotIds().contains(id)) {
                Query query = Query.query(Criteria.where("copilotId").is(id));
                Update update = new Update();
                // 增加一次views
                update.inc("views");
                mongoTemplate.updateFirst(query, update, Copilot.class);
                if (Objects.isNull(cache)) {
                    redisCache.setCache("views:" + userIdOrIpAddress, new RatingCache(Sets.newHashSet(id)));
                } else {
                    redisCache.updateCache("views:" + userIdOrIpAddress, RatingCache.class, cache,
                            updateCache -> {
                                updateCache.getCopilotIds().add(id);
                                return updateCache;
                            }, 60, TimeUnit.MINUTES);
                }
            }
            CopilotRating rating = copilotRatingRepository.findByCopilotId(copilot.getCopilotId());
            Map<String, MaaUser> maaUser = userRepository.findByUsersId(List.of(copilot.getUploaderId()));

            return formatCopilot(userIdOrIpAddress, copilot, rating, maaUser.get(copilot.getUploaderId()).getUserName(),
                    commentsAreaRepository.countByCopilotIdAndDelete(copilot.getCopilotId(), false));
        });
    }

    /**
     * 分页查询。传入 userId 不为空时限制为用户所有的数据
     *
     * @param userId  获取已登录用户自己的作业数据
     * @param request 模糊查询
     * @return CopilotPageInfo
     */
    public CopilotPageInfo queriesCopilot(@Nullable String userId, CopilotQueriesRequest request) {
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

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortOrder));

        Query queryObj = new Query();
        Criteria criteriaObj = new Criteria();

        Set<Criteria> andQueries = new HashSet<>();
        Set<Criteria> norQueries = new HashSet<>();
        Set<Criteria> orQueries = new HashSet<>();

        andQueries.add(Criteria.where("delete").is(false));


        //关卡名、关卡类型、关卡编号
        if (StringUtils.isNotBlank(request.getLevelKeyword())) {
            List<ArkLevelInfo> levelInfo = levelService.queryLevelByKeyword(request.getLevelKeyword());
            if (levelInfo.isEmpty()) {
                andQueries.add(Criteria.where("stageName").regex(caseInsensitive(request.getLevelKeyword())));
            } else {
                andQueries.add(Criteria.where("stageName").in(levelInfo.stream()
                        .map(ArkLevelInfo::getStageId).collect(Collectors.toSet())));
            }
        }

        //标题、描述、神秘代码
        if (StringUtils.isNotBlank(request.getDocument())) {
            orQueries.add(Criteria.where("doc.title").regex(caseInsensitive(request.getDocument())));
            orQueries.add(Criteria.where("doc.details").regex(caseInsensitive(request.getDocument())));
        }


        //包含或排除干员
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

        //查看自己
        if (StringUtils.isNotBlank(request.getUploaderId())) {
            if ("me".equals(request.getUploaderId())) {
                if (!ObjectUtils.isEmpty(userId)) {
                    andQueries.add(Criteria.where("uploaderId").is(userId));
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
        Map<String, MaaUser> maaUsers = userRepository.findByUsersId(copilots.stream().map(Copilot::getUploaderId).toList());
        Map<Long, Long> commentsCount = commentsAreaRepository.findByCopilotIdInAndDelete(copilotIds, false)
                .collect(Collectors.groupingBy(CommentsArea::getCopilotId, Collectors.counting()));
        List<CopilotInfo> infos = copilots.stream().map(copilot ->
                        formatCopilot(userId, copilot,
                                ratingByCopilotId.get(copilot.getCopilotId()),
                                maaUsers.get(copilot.getUploaderId()).getUserName(),
                                commentsCount.get(copilot.getCopilotId())))
                .toList();


        // 计算页面
        int pageNumber = (int) Math.ceil((double) count / limit);

        // 判断是否存在下一页
        boolean hasNext = count - (long) page * limit > 0;

        // 封装数据
        return new CopilotPageInfo()
                .setTotal(count)
                .setHasNext(hasNext)
                .setData(infos)
                .setPage(pageNumber);
    }

    /**
     * 增量更新
     *
     * @param copilotCUDRequest 作业_id content
     */
    public void update(String loginUserId, CopilotCUDRequest copilotCUDRequest) {
        String content = copilotCUDRequest.getContent();
        Long id = copilotCUDRequest.getId();
        copilotRepository.findByCopilotId(id).ifPresent(copilot -> {
            CopilotDTO copilotDTO = correctCopilot(parseToCopilotDto(content));
            Assert.state(Objects.equals(copilot.getUploaderId(), loginUserId), "您无法修改不属于您的作业");
            copilot.setUploadTime(new Date());
            CopilotConverter.INSTANCE.updateCopilotFromDto(copilotDTO, content, copilot);
            copilotRepository.save(copilot);
        });
    }

    /**
     * 评分相关
     *
     * @param request           评分
     * @param userIdOrIpAddress 用于已登录用户作出评分
     */
    public void rates(String userIdOrIpAddress, CopilotRatingReq request) {
        String rating = request.getRating();

        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(request.getId()), "作业id不存在");

        //评分表不存在 创建评分表
        if (!copilotRatingRepository.existsCopilotRatingByCopilotId(request.getId())) {
            CopilotRating copilotRating = new CopilotRating(request.getId());
            copilotRating.setRatingUsers(
                    List.of(
                            new CopilotRating.RatingUser(userIdOrIpAddress, request.getRating())
                    )
            );
            copilotRatingRepository.insert(copilotRating);
        }


        // 获取评分表
        Query query = Query.query(Criteria.where("copilotId").is(request.getId()));
        Update update = new Update();

        // 查询指定作业评分
        CopilotRating copilotRating = copilotRatingRepository.findByCopilotId(request.getId());

        boolean existUserId = false;

        List<CopilotRating.RatingUser> ratingUsers = copilotRating.getRatingUsers();

        // 查看是否已评分 如果已评分则进行更新
        for (CopilotRating.RatingUser ratingUser : ratingUsers) {
            if (userIdOrIpAddress.equals(ratingUser.getUserId())) {
                //做出相同的评分则直接返回
                if (ratingUser.getRating().equals(rating)) {
                    return;
                }
                existUserId = true;
                ratingUser.setRating(rating);
            }
        }

        copilotRating.setRatingUsers(ratingUsers);
        mongoTemplate.save(copilotRating);

        // 不存在评分 则添加新的评分
        if (!existUserId) {
            CopilotRating.RatingUser ratingUser = new CopilotRating.RatingUser(userIdOrIpAddress, rating);
            ratingUsers.add(ratingUser);
            update.addToSet("ratingUsers", ratingUser);
            mongoTemplate.updateFirst(query, update, CopilotRating.class);
        }


        List<CopilotRating.RatingUser> newRatingUsers = copilotRatingRepository.findByCopilotId(request.getId()).getRatingUsers();
        // 计算评分相关
        long ratingCount = newRatingUsers.stream().filter(ratingUser ->
                        Objects.equals(ratingUser.getRating(), "Like") || Objects.equals(ratingUser.getRating(), "Dislike"))
                .count();

        long likeCount = newRatingUsers.stream().filter(ratingUser ->
                Objects.equals(ratingUser.getRating(), "Like")).count();

        long disLikeCount = newRatingUsers.stream().filter(ratingUser ->
                Objects.equals(ratingUser.getRating(), "Dislike")).count();


        double rawRatingLevel = ratingCount != 0 ? (double) likeCount / ratingCount : 0;
        BigDecimal bigDecimal = new BigDecimal(rawRatingLevel);
        // 只取一位小数点
        double ratingLevel = bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
        // 更新数据
        copilotRating.setRatingUsers(ratingUsers);
        copilotRating.setRatingLevel((int) (ratingLevel * 10));
        copilotRating.setRatingRatio(ratingLevel);
        mongoTemplate.save(copilotRating);

        // TODO 计算热度 (暂时) 评分达到指定阈值 添加额外分数 使其排名更高
        double hotScore = ratingCount > 5 ? rawRatingLevel + (likeCount - disLikeCount) + 10 : rawRatingLevel + (likeCount - disLikeCount);
        // 更新热度
        copilotRepository.findByCopilotId(request.getId()).ifPresent(copilot ->
                copilotRepository.save(copilot.setHotScore(hotScore)));

    }

    /**
     * 将数据库内容转换为前端所需格式<br>
     */
    private CopilotInfo formatCopilot(String userIdOrIpAddress, Copilot copilot, CopilotRating rating, String userName,
                                      Long commentsCount) {
        CopilotInfo info = CopilotConverter.INSTANCE.toCopilotInfo(copilot, userName, copilot.getCopilotId(),
                commentsCount);
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
                    .filter(ru -> Objects.equals(userIdOrIpAddress, ru.getUserId()))
                    .findFirst()
                    .ifPresent(fst -> info.setRatingType(RatingType.fromRatingType(fst.getRating()).getDisplay()));
        });

        info.setAvailable(true);

        try {
            // 兼容客户端, 将作业ID替换为数字ID
            copilot.setId(Long.toString(copilot.getCopilotId()));
            if (StringUtils.isEmpty(info.getContent())) {
                // 设置干员组干员信息
                if (copilot.getGroups() != null) {
                    copilot.getGroups()
                            .forEach(group -> {
                                List<String> strings = group.getOpers().stream()
                                        .map(opera -> String.format("%s %s", opera.getName(), opera.getSkill()))
                                        .toList();
                                group.setOperators(strings);
                            });
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
        copilotRepository.findByCopilotId(copilotId).ifPresent(copilot ->
                copilotRepository.save(copilot.setContent(content)));
    }
}