package plus.maa.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import plus.maa.backend.controller.request.copilot.CopilotCUDRequest;
import plus.maa.backend.controller.request.copilot.CopilotDTO;
import plus.maa.backend.controller.request.copilot.CopilotQueriesRequest;
import plus.maa.backend.controller.request.copilot.CopilotRatingReq;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.controller.response.copilot.CopilotInfo;
import plus.maa.backend.controller.response.copilot.CopilotPageInfo;
import plus.maa.backend.repository.*;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.repository.entity.Rating;
import plus.maa.backend.service.model.RatingCache;
import plus.maa.backend.service.model.RatingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
    private final RatingRepository ratingRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper mapper;
    private final ArkLevelService levelService;
    private final RedisCache redisCache;
    private final UserRepository userRepository;
    private final CommentsAreaRepository commentsAreaRepository;

    private final CopilotConverter copilotConverter;
    private final AtomicLong copilotIncrementId = new AtomicLong(20000);

    /*
        首页分页查询缓存配置
        格式为：需要缓存的 orderBy 类型（也就是榜单类型） -> 缓存时间
        （Map.of()返回的是不可变对象，无需担心线程安全问题）
     */
    private static final Map<String, Long> HOME_PAGE_CACHE_CONFIG = Map.of(
            "hot", 3600 * 24L,
            "views", 3600L,
            "id", 300L
    );

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
        Copilot copilot = copilotConverter.toCopilot(
                copilotDTO, loginUserId,
                LocalDateTime.now(), copilotIncrementId.getAndIncrement(),
                content);
        copilotRepository.insert(copilot);
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
            /*
             * 删除作业时，如果被删除的项在 Redis 首页缓存中存在，则清空对应的首页缓存
             * 新增作业就不必，因为新作业显然不会那么快就登上热度榜和浏览量榜
             */
            for (var kv : HOME_PAGE_CACHE_CONFIG.entrySet()) {
                String key = String.format("home:%s:copilotIds", kv.getKey());
                String pattern = String.format("home:%s:*", kv.getKey());
                if (redisCache.valueMemberInSet(key, copilot.getCopilotId())) {
                    redisCache.removeCacheByPattern(pattern);
                }
            }
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
            Map<String, MaaUser> maaUser = userRepository.findByUsersId(List.of(copilot.getUploaderId()));

            // 新评分系统
            RatingType ratingType = ratingRepository.findByTypeAndKeyAndUserId(Rating.KeyType.COPILOT,
                    Long.toString(copilot.getCopilotId()), userIdOrIpAddress)
                    .map(Rating::getRating)
                    .orElse(null);
            // 用户点进作业会显示点赞信息
            return formatCopilot(copilot, ratingType, maaUser.get(copilot.getUploaderId()).getUserName(),
                    commentsAreaRepository.countByCopilotIdAndDelete(copilot.getCopilotId(), false));
        });
    }

    /**
     * 分页查询。传入 userId 不为空时限制为用户所有的数据
     * 会缓存默认状态下热度和访问量排序的结果
     *
     * @param userId  获取已登录用户自己的作业数据
     * @param request 模糊查询
     * @return CopilotPageInfo
     */
    public CopilotPageInfo queriesCopilot(@Nullable String userId, CopilotQueriesRequest request) {

        AtomicLong cacheTimeout = new AtomicLong();
        AtomicReference<String> cacheKey = new AtomicReference<>();
        AtomicReference<String> setKey = new AtomicReference<>();
        // 只缓存默认状态下热度和访问量排序的结果，并且最多只缓存前三页
        if (request.getPage() <= 3 && request.getDocument() == null && request.getLevelKeyword() == null &&
                request.getUploaderId() == null && request.getOperator() == null) {

            Optional<CopilotPageInfo> cacheOptional = Optional.ofNullable(request.getOrderBy())
                    .filter(StringUtils::isNotBlank)
                    .map(HOME_PAGE_CACHE_CONFIG::get)
                    .map(t -> {
                        cacheTimeout.set(t);
                        setKey.set(String.format("home:%s:copilotIds", request.getOrderBy()));
                        cacheKey.set(String.format("home:%s:%s", request.getOrderBy(), request.hashCode()));
                        return redisCache.getCache(cacheKey.get(), CopilotPageInfo.class);
                    });

            // 如果缓存存在则直接返回
            if (cacheOptional.isPresent()) {
                return cacheOptional.get();
            }
        }

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
        if (!andQueries.isEmpty()) {
            criteriaObj.andOperator(andQueries);
        }
        if (!norQueries.isEmpty()) {
            criteriaObj.norOperator(norQueries);
        }
        if (!orQueries.isEmpty()) {
            criteriaObj.orOperator(orQueries);
        }
        queryObj.addCriteria(criteriaObj);
        // 查询总数
        long count = mongoTemplate.count(queryObj, Copilot.class);

        // 分页排序查询
        List<Copilot> copilots = mongoTemplate.find(queryObj.with(pageable), Copilot.class);


        // 填充前端所需信息
        Set<Long> copilotIds = copilots.stream().map(Copilot::getCopilotId).collect(Collectors.toSet());
        Map<String, MaaUser> maaUsers = userRepository.findByUsersId(copilots.stream().map(Copilot::getUploaderId).toList());
        Map<Long, Long> commentsCount = commentsAreaRepository.findByCopilotIdInAndDelete(copilotIds, false)
                .collect(Collectors.groupingBy(CommentsArea::getCopilotId, Collectors.counting()));

        // 新版评分系统
        // 反正目前首页和搜索不会直接展示当前用户有没有点赞，干脆直接不查，要用户点进作业才显示自己是否点赞
        List<CopilotInfo> infos = copilots.stream().map(copilot ->
                formatCopilot(copilot, null,
                        maaUsers.get(copilot.getUploaderId()).getUserName(),
                        commentsCount.get(copilot.getCopilotId())))
                .toList();

        // 计算页面
        int pageNumber = (int) Math.ceil((double) count / limit);

        // 判断是否存在下一页
        boolean hasNext = count - (long) page * limit > 0;

        // 封装数据
        CopilotPageInfo data = new CopilotPageInfo()
                .setTotal(count)
                .setHasNext(hasNext)
                .setData(infos)
                .setPage(pageNumber);

        // 决定是否缓存
        if (cacheKey.get() != null) {
            // 记录存在的作业id
            redisCache.addSet(setKey.get(), copilotIds, cacheTimeout.get());
            // 缓存数据
            redisCache.setCache(cacheKey.get(), data, cacheTimeout.get());
        }
        return data;
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
            copilot.setUploadTime(LocalDateTime.now());
            copilotConverter.updateCopilotFromDto(copilotDTO, content, copilot);
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

        int likeCountChange = 0;
        int dislikeCountChange = 0;
        Optional<Rating> ratingOptional = ratingRepository.findByTypeAndKeyAndUserId(Rating.KeyType.COPILOT,
                Long.toString(request.getId()), userIdOrIpAddress);
        // 如果评分存在则更新评分
        if (ratingOptional.isPresent()) {
            Rating rating1 = ratingOptional.get();
            // 如果评分相同，则不做任何操作
            if (Objects.equals(rating1.getRating(), RatingType.fromRatingType(rating))) {
                return;
            }
            // 如果评分不同则更新评分
            RatingType oldRatingType = rating1.getRating();
            rating1.setRating(RatingType.fromRatingType(rating));
            rating1.setRateTime(LocalDateTime.now());
            ratingRepository.save(rating1);
            // 计算评分变化
            likeCountChange = rating1.getRating() == RatingType.LIKE ? 1 :
                    (oldRatingType != RatingType.LIKE ? 0 : -1);
            dislikeCountChange = rating1.getRating() == RatingType.DISLIKE ? 1 :
                    (oldRatingType != RatingType.DISLIKE ? 0 : -1);
        }

        // 不存在评分 则添加新的评分
        if (ratingOptional.isEmpty()) {
            Rating newRating = new Rating()
                    .setType(Rating.KeyType.COPILOT)
                    .setKey(Long.toString(request.getId()))
                    .setUserId(userIdOrIpAddress)
                    .setRating(RatingType.fromRatingType(rating))
                    .setRateTime(LocalDateTime.now());

            ratingRepository.insert(newRating);
            // 计算评分变化
            likeCountChange = newRating.getRating() == RatingType.LIKE ? 1 : 0;
            dislikeCountChange = newRating.getRating() == RatingType.DISLIKE ? 1 : 0;
        }

        // 获取只包含评分的作业
        Query query = Query.query(Criteria
                .where("copilotId").is(request.getId())
                .and("delete").is(false)
        );
        // 排除 _id，防止误 save 该不完整作业后原有数据丢失
        query.fields().include("likeCount", "dislikeCount").exclude("_id");
        Copilot copilot = mongoTemplate.findOne(query, Copilot.class);
        Assert.notNull(copilot, "作业不存在");

        // 计算评分相关
        long likeCount = copilot.getLikeCount() + likeCountChange;
        likeCount = likeCount < 0 ? 0 : likeCount;
        long ratingCount = likeCount + copilot.getDislikeCount() + dislikeCountChange;
        ratingCount = ratingCount < 0 ? 0 : ratingCount;

        double rawRatingLevel = ratingCount != 0 ? (double) likeCount / ratingCount : 0;
        BigDecimal bigDecimal = new BigDecimal(rawRatingLevel);
        // 只取一位小数点
        double ratingLevel = bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
        // 更新数据
        query = Query.query(Criteria
                .where("copilotId").is(request.getId())
                .and("delete").is(false)
        );
        Update update = new Update();
        update.set("likeCount", likeCount);
        update.set("dislikeCount", ratingCount - likeCount);
        update.set("ratingLevel", (int) (ratingLevel * 10));
        update.set("ratingRatio", ratingLevel);
        mongoTemplate.updateFirst(query, update, Copilot.class);
    }

    public static double getHotScore(Copilot copilot, long lastWeekLike, long lastWeekDislike) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime uploadTime = copilot.getUploadTime();
        // 基于时间的基础分
        double base = 6d;
        // 相比上传时间过了多少周
        long pastedWeeks = ChronoUnit.WEEKS.between(uploadTime, now) + 1;
        base = base / Math.log(pastedWeeks + 1);
        // 上一周好评率
        long ups = Math.max(lastWeekLike, 1);
        long downs = Math.max(lastWeekDislike, 0);
        double greatRate = (double) ups / (ups + downs);
        if ((ups + downs) >= 5 && downs >= ups) {
            // 将信赖就差评过多的作业打入地狱
            base = base * greatRate;
        }
        // 上一周好评率 * (上一周评分数 / 10) * (浏览数 / 10) / 过去的周数
        double s = greatRate * (copilot.getViews() / 10d)
                * Math.max((ups + downs) / 10d, 1) / pastedWeeks;
        double order = Math.log(Math.max(s, 1));
        return order + s / 1000d + base;
    }

    /**
     * 将数据库内容转换为前端所需格式 <br>
     * 新版评分系统
     */
    private CopilotInfo formatCopilot(Copilot copilot, @Nullable RatingType ratingType, String userName,
                                      Long commentsCount) {
        CopilotInfo info = copilotConverter.toCopilotInfo(copilot, userName, copilot.getCopilotId(),
                commentsCount);

        info.setRatingRatio(copilot.getRatingRatio());
        info.setRatingLevel(copilot.getRatingLevel());
        if (ratingType != null) {
            info.setRatingType(ratingType.getDisplay());
        }
        // 评分数少于一定数量
        info.setNotEnoughRating(copilot.getLikeCount() + copilot.getDislikeCount() <= 5);

        info.setAvailable(true);

        // 兼容客户端, 将作业ID替换为数字ID
        copilot.setId(Long.toString(copilot.getCopilotId()));
        return info;
    }

    public void notificationStatus(String userId, Long copilotId, boolean status) {
        Optional<Copilot> copilotOptional = copilotRepository.findByCopilotId(copilotId);
        Assert.isTrue(copilotOptional.isPresent(), "copilot不存在");
        Copilot copilot = copilotOptional.get();
        Assert.isTrue(Objects.equals(userId, copilot.getUploaderId()), "您没有权限修改");
        copilot.setNotification(status);
        copilotRepository.save(copilot);
    }
}