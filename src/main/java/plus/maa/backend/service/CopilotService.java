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
import plus.maa.backend.repository.*;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.LoginUser;
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

    private final HttpServletRequest request;

    private final RedisCache redisCache;

    private final TableLogicDelete tableLogicDelete;

    private final UserRepository userRepository;
    private final CopilotRatingRepository copilotRatingRepository;
    private final AtomicLong copilotIncrementId = new AtomicLong(20000);

    @PostConstruct
    public void init() {
        // ?????????copilotId, ??????????????????????????????copilotId
        // ??????????????????????????????, ??????20000??????
        copilotRepository.findFirstByOrderByCopilotIdDesc()
                .map(Copilot::getCopilotId)
                .ifPresent(last -> copilotIncrementId.set(last + 1));

        log.info("????????????ID???????????????: {}", copilotIncrementId.get());
    }

    /**
     * ??????_id??????Copilot
     *
     * @param id _id
     * @return Copilot
     */
    private Copilot findById(String id) {
        return StringUtils.isNumeric(id) ?
                copilotRepository.findByCopilotIdAndDeleteIsFalse(Long.parseLong(id)).orElseThrow(() -> new MaaResultException("???????????????"))
                : copilotRepository.findByIdAndDeleteIsFalse(id).orElseThrow(() -> new MaaResultException("???????????????"));
    }

    private Copilot findById(Long id) {
        return findById(id.toString());
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param operationId ??????id
     */
    private void verifyOwner(LoginUser user, String operationId) {
        Assert.hasText(operationId, "??????id????????????");
        String userId = user.getMaaUser().getUserId();
        Copilot copilot = findById(operationId);
        Assert.state(Objects.equals(copilot.getUploaderId(), userId), "????????????????????????????????????");
    }

    /**
     * ????????????????????????
     * ??????????????????????????????
     *
     * @param copilotDTO copilotDTO
     */
    private CopilotDTO correctCopilot(CopilotDTO copilotDTO) {

        // ??????name???????????????
        // todo ?????????????????????????????????
        if (copilotDTO.getGroups() != null) {
            copilotDTO.getGroups().forEach(
                    group -> {
                        if (group.getOpers() != null) {
                            group.getOpers().forEach(oper -> oper
                                    .setName(oper.getName() == null ? null : oper.getName().replaceAll("[\"??????]", "")));
                        }
                    });
        }
        if (copilotDTO.getOpers() != null) {
            copilotDTO.getOpers().forEach(operator -> operator
                    .setName(operator.getName() == null ? null : operator.getName().replaceAll("[\"??????]", "")));
        }

        // actions name ????????????
        if (copilotDTO.getActions() != null) {
            copilotDTO.getActions().forEach(action -> action
                    .setName(action.getName() == null ? null : action.getName().replaceAll("[\"??????]", "")));
        }
        // ??????stageId????????????????????????
        ArkLevelInfo level = levelService.findByLevelIdFuzzy(copilotDTO.getStageName());
        if (level != null) {
            copilotDTO.setStageName(level.getStageId());
        }
        return copilotDTO;
    }

    /**
     * ???content?????????CopilotDTO
     *
     * @param content content
     * @return CopilotDTO
     */
    private CopilotDTO parseToCopilotDto(String content) {
        Assert.notNull(content, "????????????????????????");
        try {
            return mapper.readValue(content, CopilotDTO.class);
        } catch (JsonProcessingException e) {
            log.error("??????copilot??????", e);
            throw new MaaResultException("??????copilot??????");
        }
    }


    private Pattern caseInsensitive(String s) {
        return Pattern.compile(s, Pattern.CASE_INSENSITIVE);
    }


    /**
     * ??????????????????
     *
     * @param content ????????????json????????????
     * @return ??????_id
     */
    public MaaResult<Long> upload(LoginUser user, String content) {
        CopilotDTO copilotDTO = correctCopilot(parseToCopilotDto(content));
        // ????????????????????????????????????
        Copilot copilot = CopilotConverter.INSTANCE.toCopilot(
                copilotDTO, user.getMaaUser(),
                new Date(), copilotIncrementId.getAndIncrement(),
                content);
        copilotRepository.insert(copilot);
        copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId()));
        return MaaResult.success(copilot.getCopilotId());
    }

    /**
     * ????????????id????????????
     */
    public MaaResult<Void> delete(LoginUser user, CopilotCUDRequest request) {
        String operationId = request.getId();
        verifyOwner(user, operationId);
        tableLogicDelete.deleteCopilotById(operationId);
        return MaaResult.success();
    }

    /**
     * ????????????
     */
    public Optional<CopilotInfo> getCopilotById(LoginUser user, Long id) {
        String userId = getUserId(user);
        // ??????ID????????????, ???????????????????????????????????????
        Optional<Copilot> copilotOptional = copilotRepository.findByCopilotIdAndDeleteIsFalse(id);
        return copilotOptional.map(copilot -> {
            // 60???????????????????????????????????????????????????
            RatingCache cache = redisCache.getCache("views:" + userId, RatingCache.class);
            if (Objects.isNull(cache) || Objects.isNull(cache.getCopilotIds()) ||
                    !cache.getCopilotIds().contains(id)) {
                Query query = Query.query(Criteria.where("copilotId").is(id));
                Update update = new Update();
                // ????????????views
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
            Map<String, MaaUser> maaUser = userRepository.findByUsersId(List.of(copilot.getUploaderId()));
            return formatCopilot(userId, copilot, rating, maaUser.get(copilot.getUploaderId()).getUserName());
        });
    }

    /**
     * ????????????
     *
     * @param user    ??????????????????????????????????????????
     * @param request ????????????
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
        // ?????????????????? ??????????????????
        int page = request.getPage() > 0 ? request.getPage() : 1;
        int limit = request.getLimit() > 0 ? request.getLimit() : 10;

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortOrder));

        Query queryObj = new Query();
        Criteria criteriaObj = new Criteria();

        Set<Criteria> andQueries = new HashSet<>();
        Set<Criteria> norQueries = new HashSet<>();
        Set<Criteria> orQueries = new HashSet<>();

        andQueries.add(Criteria.where("delete").is(false));


        //???????????????????????????????????????
        if (StringUtils.isNotBlank(request.getLevelKeyword())) {
            List<ArkLevelInfo> levelInfo = levelService.queryLevelByKeyword(request.getLevelKeyword());
            if (levelInfo.isEmpty()) {
                andQueries.add(Criteria.where("stageName").regex(caseInsensitive(request.getLevelKeyword())));
            } else {
                andQueries.add(Criteria.where("stageName").in(levelInfo.stream()
                        .map(ArkLevelInfo::getStageId).collect(Collectors.toSet())));
            }
        }

        //??????????????????????????????
        if (StringUtils.isNotBlank(request.getDocument())) {
            orQueries.add(Criteria.where("doc.title").regex(caseInsensitive(request.getDocument())));
            orQueries.add(Criteria.where("doc.details").regex(caseInsensitive(request.getDocument())));
        }


        //?????????????????????
        String oper = request.getOperator();
        if (StringUtils.isNotBlank(oper)) {
            oper = oper.replaceAll("[???\"???]", "");
            String[] operators = oper.split(",");
            for (String operator : operators) {
                if (operator.startsWith("~")) {
                    String exclude = operator.substring(1);
                    // ????????????????????????
                    norQueries.add(Criteria.where("opers.name").regex(exclude));
                } else {
                    // ??????????????????????????????
                    andQueries.add(Criteria.where("opers.name").regex(operator));
                }
            }
        }

        //????????????
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

        // ????????????
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
        // ????????????
        long count = mongoTemplate.count(queryObj, Copilot.class);

        // ??????????????????
        List<Copilot> copilots = mongoTemplate.find(queryObj.with(pageable), Copilot.class);


        // ????????????????????????
        Set<Long> copilotIds = copilots.stream().map(Copilot::getCopilotId).collect(Collectors.toSet());
        List<CopilotRating> ratings = copilotRatingRepository.findByCopilotIdIn(copilotIds);
        Map<Long, CopilotRating> ratingByCopilotId = Maps.uniqueIndex(ratings, CopilotRating::getCopilotId);
        Map<String, MaaUser> maaUsers = userRepository.findByUsersId(copilots.stream().map(Copilot::getUploaderId).toList());
        List<CopilotInfo> infos = copilots.stream().map(copilot -> formatCopilot(userId, copilot,
                        ratingByCopilotId.get(copilot.getCopilotId()), maaUsers.get(copilot.getUploaderId()).getUserName()))
                .toList();


        // ????????????
        int pageNumber = (int) Math.ceil((double) count / limit);

        // ???????????????????????????
        boolean hasNext = count - (long) page * limit > 0;


        // ????????????
        CopilotPageInfo copilotPageInfo = new CopilotPageInfo();
        copilotPageInfo.setTotal(count)
                .setHasNext(hasNext)
                .setData(infos)
                .setPage(pageNumber);
        return MaaResult.success(copilotPageInfo);
    }

    /**
     * ????????????
     *
     * @param copilotCUDRequest ??????_id content
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
     * ????????????
     *
     * @param request   ??????
     * @param loginUser ?????????????????????????????????
     * @return null
     */
    public MaaResult<String> rates(LoginUser loginUser, CopilotRatingReq request) {
        String userId = getUserId(loginUser);
        String rating = request.getRating();

        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(request.getId()), "??????id?????????");

        //?????????????????? ???????????????
        if (!copilotRatingRepository.existsCopilotRatingByCopilotId(request.getId())) {
            CopilotRating copilotRating = new CopilotRating(request.getId());
            copilotRating.setRatingUsers(
                    List.of(
                            new CopilotRating.RatingUser(userId, request.getRating())
                    )
            );
            copilotRatingRepository.insert(copilotRating);
        }


        // ???????????????
        Query query = Query.query(Criteria.where("copilotId").is(request.getId()));
        Update update = new Update();

        // ????????????????????????
        CopilotRating copilotRating = copilotRatingRepository.findByCopilotId(request.getId());

        boolean existUserId = false;

        List<CopilotRating.RatingUser> ratingUsers = copilotRating.getRatingUsers();

        // ????????????????????? ??????????????????????????????
        for (CopilotRating.RatingUser ratingUser : ratingUsers) {
            if (userId.equals(ratingUser.getUserId())) {
                //????????????????????????????????????
                if (ratingUser.getRating().equals(rating)) {
                    return MaaResult.success("????????????");
                }
                existUserId = true;
                ratingUser.setRating(rating);
            }
        }

        copilotRating.setRatingUsers(ratingUsers);
        mongoTemplate.save(copilotRating);

        // ??????????????? ?????????????????????
        if (!existUserId) {
            CopilotRating.RatingUser ratingUser = new CopilotRating.RatingUser(userId, rating);
            ratingUsers.add(ratingUser);
            update.addToSet("ratingUsers", ratingUser);
            mongoTemplate.updateFirst(query, update, CopilotRating.class);
        }


        List<CopilotRating.RatingUser> newRatingUsers = copilotRatingRepository.findByCopilotId(request.getId()).getRatingUsers();
        // ??????????????????
        long ratingCount = newRatingUsers.stream().filter(ratingUser ->
                        Objects.equals(ratingUser.getRating(), "Like") || Objects.equals(ratingUser.getRating(), "Dislike"))
                .count();

        long likeCount = newRatingUsers.stream().filter(ratingUser ->
                Objects.equals(ratingUser.getRating(), "Like")).count();

        long disLikeCount = newRatingUsers.stream().filter(ratingUser ->
                Objects.equals(ratingUser.getRating(), "Dislike")).count();


        double rawRatingLevel = ratingCount != 0 ? (double) likeCount / ratingCount : 0;
        BigDecimal bigDecimal = new BigDecimal(rawRatingLevel);
        // ?????????????????????
        double ratingLevel = bigDecimal.setScale(1, RoundingMode.HALF_UP).doubleValue();
        // ????????????
        copilotRating.setRatingUsers(ratingUsers);
        copilotRating.setRatingLevel((int) (ratingLevel * 10));
        copilotRating.setRatingRatio(ratingLevel);
        mongoTemplate.save(copilotRating);

        // TODO ???????????? (??????) ???????????????????????? ?????????????????? ??????????????????
        double hotScore = ratingCount > 5 ? rawRatingLevel + (likeCount - disLikeCount) + 10 : rawRatingLevel + (likeCount - disLikeCount);
        // ????????????
        mongoTemplate.save(findById(request.getId()).setHotScore(hotScore));

        return MaaResult.success("????????????");
    }

    /**
     * ?????????????????????????????????<br/>
     * ???????????????..
     *
     * @return null
     */
    public MaaResult<Void> refactorExistingDatabase() {
        List<Copilot> all = copilotRepository.findAll();
        List<Copilot> notExistRatingTable = all.stream()
                .filter(a -> !copilotRatingRepository.existsCopilotRatingByCopilotId(a.getCopilotId())).toList();
        notExistRatingTable
                .forEach(copilot -> copilotRatingRepository.insert(new CopilotRating(copilot.getCopilotId())));
        // ????????????????????????
        copilotRepository.saveAll(all);
        return MaaResult.success(null);
    }

    /**
     * ?????????????????????????????????????????????<br>
     */
    private CopilotInfo formatCopilot(String userId, Copilot copilot, CopilotRating rating, String userName) {
        CopilotInfo info = CopilotConverter.INSTANCE.toCopilotInfo(copilot, userName, copilot.getCopilotId());
        Optional<CopilotRating> copilotRating = Optional.ofNullable(rating);

        // ???????????????????????????????????????????????? ??????????????????????????????????????? 0 = None 1 = LIKE 2 = DISLIKE
        copilotRating.map(cr -> {
            info.setRatingRatio(cr.getRatingRatio());
            info.setRatingLevel(cr.getRatingLevel());
            return cr.getRatingUsers();
        }).ifPresent(rus -> {
            // ???????????????????????????
            info.setNotEnoughRating(rus.size() <= 5);
            rus.stream()
                    .filter(ru -> Objects.equals(userId, ru.getUserId()))
                    .findFirst()
                    .ifPresent(fst -> info.setRatingType(RatingType.fromRatingType(fst.getRating()).getDisplay()));
        });

        info.setAvailable(true);

        try {
            // ???????????????, ?????????ID???????????????ID
            copilot.setId(Long.toString(copilot.getCopilotId()));
            if (StringUtils.isEmpty(info.getContent())) {
                // ???????????????????????????
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
            log.error("json???????????????", e);
        }
        return info;
    }

    private void updateContent(Long copilotId, String content) {
        copilotRepository.save(findById(copilotId).setContent(content));
    }

    /**
     * ???????????????????????????<br/>
     * ?????????????????????ip <br/>
     * ?????????????????????id
     *
     * @param loginUser LoginUser
     * @return ???????????????
     */
    private String getUserId(LoginUser loginUser) {
        return ObjectUtils.isEmpty(loginUser) ?
                IpUtil.getIpAddr(request)
                : loginUser.getMaaUser().getUserId();

    }
}