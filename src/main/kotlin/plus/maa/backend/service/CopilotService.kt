package plus.maa.backend.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Sets
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.ObjectUtils
import plus.maa.backend.common.utils.IdComponent
import plus.maa.backend.common.utils.converter.CopilotConverter
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.request.copilot.CopilotCUDRequest
import plus.maa.backend.controller.request.copilot.CopilotDTO
import plus.maa.backend.controller.request.copilot.CopilotQueriesRequest
import plus.maa.backend.controller.request.copilot.CopilotRatingReq
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.controller.response.copilot.CopilotInfo
import plus.maa.backend.controller.response.copilot.CopilotPageInfo
import plus.maa.backend.repository.*
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.Copilot.OperationGroup
import plus.maa.backend.repository.entity.Rating
import plus.maa.backend.service.model.RatingCache
import plus.maa.backend.service.model.RatingType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max

private val log = KotlinLogging.logger { }

/**
 * @author LoMu
 * Date 2022-12-25 19:57
 */
@Service
class CopilotService(
    private val copilotRepository: CopilotRepository,
    private val ratingRepository: RatingRepository,
    private val mongoTemplate: MongoTemplate,
    private val mapper: ObjectMapper,
    private val levelService: ArkLevelService,
    private val redisCache: RedisCache,
    private val idComponent: IdComponent,
    private val userRepository: UserRepository,
    private val commentsAreaRepository: CommentsAreaRepository,
    private val properties: MaaCopilotProperties,
    private val copilotConverter: CopilotConverter
) {

    /**
     * 并修正前端的冗余部分
     *
     * @param copilotDTO copilotDTO
     */
    private fun correctCopilot(copilotDTO: CopilotDTO): CopilotDTO {
        // 去除name的冗余部分
        if (copilotDTO.groups != null) {
            copilotDTO.groups.forEach { group: Copilot.Groups ->
                if (group.opers != null) {
                    group.opers.forEach { oper: OperationGroup ->
                        oper.name = oper.name?.replace("[\"“”]".toRegex(), "")
                    }
                }
            }
        }
        if (copilotDTO.opers != null) {
            copilotDTO.opers.forEach { operator: Copilot.Operators ->
                operator.name = operator.name?.replace("[\"“”]".toRegex(), "")
            }
        }

        // actions name 不是必须
        if (copilotDTO.actions != null) {
            copilotDTO.actions.forEach { action: Copilot.Action ->
                action.name = if (action.name == null) null else action.name.replace("[\"“”]".toRegex(), "")
            }
        }
        // 使用stageId存储作业关卡信息
        val level = levelService.findByLevelIdFuzzy(copilotDTO.stageName)
        level?.stageId?.let {
            copilotDTO.stageName = it
        }
        return copilotDTO
    }

    /**
     * 将content解析为CopilotDTO
     *
     * @param content content
     * @return CopilotDTO
     */
    private fun parseToCopilotDto(content: String?): CopilotDTO {
        requireNotNull(content) {
            "作业内容不可为空"
        }
        try {
            return mapper.readValue(content, CopilotDTO::class.java)
        } catch (e: JsonProcessingException) {
            log.error(e) { "解析copilot失败" }
            throw MaaResultException("解析copilot失败")
        }
    }


    private fun caseInsensitive(s: String): Pattern {
        return Pattern.compile(s, Pattern.CASE_INSENSITIVE)
    }


    /**
     * 上传新的作业
     *
     * @param content 前端编辑json作业内容
     * @return 返回_id
     */
    fun upload(loginUserId: String, content: String?): Long {
        val copilotDTO = correctCopilot(parseToCopilotDto(content))
        // 将其转换为数据库存储对象
        val copilot = copilotConverter.toCopilot(
            copilotDTO,
            idComponent.getId(Copilot.META), loginUserId, LocalDateTime.now(), content
        )
        copilotRepository.insert(copilot)
        return copilot.copilotId
    }

    /**
     * 根据作业id删除作业
     */
    fun delete(loginUserId: String, request: CopilotCUDRequest) {
        copilotRepository.findByCopilotId(request.id!!)?.let { copilot: Copilot ->
            Assert.state(copilot.uploaderId == loginUserId, "您无法修改不属于您的作业")
            copilot.isDelete = true
            copilotRepository.save(copilot)
            /*
             * 删除作业时，如果被删除的项在 Redis 首页缓存中存在，则清空对应的首页缓存
             * 新增作业就不必，因为新作业显然不会那么快就登上热度榜和浏览量榜
             */
            for ((key1) in HOME_PAGE_CACHE_CONFIG) {
                val key = String.format("home:%s:copilotIds", key1)
                val pattern = String.format("home:%s:*", key1)
                if (redisCache.valueMemberInSet(key, copilot.copilotId)) {
                    redisCache.removeCacheByPattern(pattern)
                }
            }
        }
    }

    /**
     * 指定查询
     */
    fun getCopilotById(userIdOrIpAddress: String, id: Long): CopilotInfo? {
        // 根据ID获取作业, 如作业不存在则抛出异常返回
        val copilotOptional = copilotRepository.findByCopilotIdAndDeleteIsFalse(id)
        return copilotOptional?.let { copilot: Copilot ->
            // 60分钟内限制同一个用户对访问量的增加
            val cache = redisCache.getCache("views:$userIdOrIpAddress", RatingCache::class.java)
            if (Objects.isNull(cache) || Objects.isNull(cache!!.copilotIds) ||
                !cache.copilotIds.contains(id)
            ) {
                val query = Query.query(Criteria.where("copilotId").`is`(id))
                val update = Update()
                // 增加一次views
                update.inc("views")
                mongoTemplate.updateFirst(query, update, Copilot::class.java)
                if (cache == null) {
                    redisCache.setCache("views:$userIdOrIpAddress", RatingCache(Sets.newHashSet(id)))
                } else {
                    redisCache.updateCache(
                        "views:$userIdOrIpAddress", RatingCache::class.java, cache,
                        { updateCache: RatingCache? ->
                            updateCache!!.copilotIds.add(id)
                            updateCache
                        }, 60, TimeUnit.MINUTES
                    )
                }
            }
            val maaUser = userRepository.findByUserId(copilot.uploaderId)

            // 新评分系统
            val ratingType = ratingRepository.findByTypeAndKeyAndUserId(
                Rating.KeyType.COPILOT,
                copilot.copilotId.toString(), userIdOrIpAddress
            )?.rating
            formatCopilot(
                copilot, ratingType, maaUser!!.userName,
                commentsAreaRepository.countByCopilotIdAndDelete(copilot.copilotId, false)
            )
        }
    }

    /**
     * 分页查询。传入 userId 不为空时限制为用户所有的数据
     * 会缓存默认状态下热度和访问量排序的结果
     *
     * @param userId  获取已登录用户自己的作业数据
     * @param request 模糊查询
     * @return CopilotPageInfo
     */
    fun queriesCopilot(userId: String?, request: CopilotQueriesRequest): CopilotPageInfo {
        val cacheTimeout = AtomicLong()
        val cacheKey = AtomicReference<String?>()
        val setKey = AtomicReference<String>()
        // 只缓存默认状态下热度和访问量排序的结果，并且最多只缓存前三页
        if (request.page <= 3 && request.document == null && request.levelKeyword == null && request.uploaderId == null && request.operator == null &&
            request.copilotIds.isNullOrEmpty()
        ) {
            request.orderBy?.takeIf { orderBy -> orderBy.isNotBlank() }
                ?.let { key -> HOME_PAGE_CACHE_CONFIG[key] }
                ?.let { t ->
                    cacheTimeout.set(t)
                    setKey.set(String.format("home:%s:copilotIds", request.orderBy))
                    cacheKey.set(String.format("home:%s:%s", request.orderBy, request.hashCode()))
                    redisCache.getCache(cacheKey.get()!!, CopilotPageInfo::class.java)
                }?.let { return it }
        }

        val sortOrder = Sort.Order(
            if (request.desc) Sort.Direction.DESC else Sort.Direction.ASC,
            request.orderBy?.takeIf { orderBy -> orderBy.isNotBlank() }?.let { ob ->
                when (ob) {
                    "hot" -> "hotScore"
                    "id" -> "copilotId"
                    else -> request.orderBy
                }
            } ?: "copilotId"
        )
        // 判断是否有值 无值则为默认
        val page = if (request.page > 0) request.page else 1
        val limit = if (request.limit > 0) request.limit else 10

        val pageable: Pageable = PageRequest.of(page - 1, limit, Sort.by(sortOrder))

        val queryObj = Query()
        val criteriaObj = Criteria()

        val andQueries: MutableSet<Criteria> = HashSet()
        val norQueries: MutableSet<Criteria> = HashSet()
        val orQueries: MutableSet<Criteria> = HashSet()

        andQueries.add(Criteria.where("delete").`is`(false))


        //关卡名、关卡类型、关卡编号
        if (!request.levelKeyword.isNullOrBlank()) {
            val keyword = request.levelKeyword!!
            val levelInfo = levelService.queryLevelInfosByKeyword(keyword)
            if (levelInfo.isEmpty()) {
                andQueries.add(Criteria.where("stageName").regex(caseInsensitive(keyword)))
            } else {
                andQueries.add(
                    Criteria.where("stageName").`in`(
                        levelInfo.map { obj: ArkLevelInfo -> obj.stageId }.toSet()
                    )
                )
            }
        }

        // 作业id列表
        if (!request.copilotIds.isNullOrEmpty()) {
            andQueries.add(Criteria.where("copilotId").`in`(request.copilotIds!!))
        }

        //标题、描述、神秘代码
        if (!request.document.isNullOrBlank()) {
            orQueries.add(Criteria.where("doc.title").regex(caseInsensitive(request.document)))
            orQueries.add(Criteria.where("doc.details").regex(caseInsensitive(request.document)))
        }


        //包含或排除干员
        var oper = request.operator
        if (!oper.isNullOrBlank()) {
            oper = oper.replace("[“\"”]".toRegex(), "")
            val operators = oper.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (operator in operators) {
                if (operator.startsWith("~")) {
                    val exclude = operator.substring(1)
                    // 排除查询指定干员
                    norQueries.add(Criteria.where("opers.name").regex(exclude))
                } else {
                    // 模糊匹配查询指定干员
                    andQueries.add(Criteria.where("opers.name").regex(operator))
                }
            }
        }

        //查看自己
        if (StringUtils.isNotBlank(request.uploaderId)) {
            if ("me" == request.uploaderId) {
                if (!ObjectUtils.isEmpty(userId)) {
                    andQueries.add(Criteria.where("uploaderId").`is`(userId))
                }
            } else {
                andQueries.add(Criteria.where("uploaderId").`is`(request.uploaderId))
            }
        }

        // 封装查询
        if (andQueries.isNotEmpty()) {
            criteriaObj.andOperator(andQueries)
        }
        if (norQueries.isNotEmpty()) {
            criteriaObj.norOperator(norQueries)
        }
        if (orQueries.isNotEmpty()) {
            criteriaObj.orOperator(orQueries)
        }
        queryObj.addCriteria(criteriaObj)
        // 查询总数
        val count = mongoTemplate.count(queryObj, Copilot::class.java)

        // 分页排序查询
        val copilots = mongoTemplate.find(queryObj.with(pageable), Copilot::class.java)

        // 填充前端所需信息
        val copilotIds = copilots.map {
            it.copilotId
        }.toSet()
        val maaUsers = userRepository.findByUsersId(copilots.map { it.uploaderId }.toList())
        val commentsCount = commentsAreaRepository.findByCopilotIdInAndDelete(copilotIds, false)
            .groupBy { it.copilotId }
            .mapValues { it.value.size.toLong() }

        // 新版评分系统
        // 反正目前首页和搜索不会直接展示当前用户有没有点赞，干脆直接不查，要用户点进作业才显示自己是否点赞
        val infos = copilots.map { copilot ->
            formatCopilot(
                copilot, null,
                maaUsers[copilot.uploaderId]!!.userName,
                commentsCount[copilot.copilotId]
            )
        }.toList()

        // 计算页面
        val pageNumber = ceil(count.toDouble() / limit).toInt()

        // 判断是否存在下一页
        val hasNext = count - page.toLong() * limit > 0

        // 封装数据
        val data = CopilotPageInfo(hasNext, pageNumber, count, infos)

        // 决定是否缓存
        if (cacheKey.get() != null) {
            // 记录存在的作业id
            redisCache.addSet(setKey.get(), copilotIds, cacheTimeout.get())
            // 缓存数据
            redisCache.setCache(cacheKey.get()!!, data, cacheTimeout.get())
        }
        return data
    }

    /**
     * 增量更新
     *
     * @param copilotCUDRequest 作业_id content
     */
    fun update(loginUserId: String, copilotCUDRequest: CopilotCUDRequest) {
        val content = copilotCUDRequest.content
        val id = copilotCUDRequest.id
        copilotRepository.findByCopilotId(id!!)?.let { copilot: Copilot ->
            val copilotDTO = correctCopilot(parseToCopilotDto(content))
            Assert.state(copilot.uploaderId == loginUserId, "您无法修改不属于您的作业")
            copilot.uploadTime = LocalDateTime.now()
            copilotConverter.updateCopilotFromDto(copilotDTO, content!!, copilot)
            copilotRepository.save(copilot)
        }
    }

    /**
     * 评分相关
     *
     * @param request           评分
     * @param userIdOrIpAddress 用于已登录用户作出评分
     */
    fun rates(userIdOrIpAddress: String, request: CopilotRatingReq) {
        val rating = request.rating

        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(request.id), "作业id不存在")

        var likeCountChange = 0
        var dislikeCountChange = 0
        val ratingOptional = ratingRepository.findByTypeAndKeyAndUserId(
            Rating.KeyType.COPILOT,
            request.id.toString(),
            userIdOrIpAddress
        )
        // 如果评分存在则更新评分
        if (ratingOptional != null) {
            // 如果评分相同，则不做任何操作
            if (ratingOptional.rating == RatingType.fromRatingType(rating)) {
                return
            }
            // 如果评分不同则更新评分
            val oldRatingType = ratingOptional.rating
            ratingOptional.rating = RatingType.fromRatingType(rating)
            ratingOptional.rateTime = LocalDateTime.now()
            ratingRepository.save(ratingOptional)
            // 计算评分变化
            likeCountChange =
                if (ratingOptional.rating == RatingType.LIKE) 1 else (if (oldRatingType != RatingType.LIKE) 0 else -1)
            dislikeCountChange =
                if (ratingOptional.rating == RatingType.DISLIKE) 1 else (if (oldRatingType != RatingType.DISLIKE) 0 else -1)
        }

        // 不存在评分 则添加新的评分
        if (ratingOptional == null) {
            val newRating = Rating(
                null,
                Rating.KeyType.COPILOT,
                request.id.toString(),
                userIdOrIpAddress,
                RatingType.fromRatingType(rating),
                LocalDateTime.now()
            )

            ratingRepository.insert(newRating)
            // 计算评分变化
            likeCountChange = if (newRating.rating == RatingType.LIKE) 1 else 0
            dislikeCountChange = if (newRating.rating == RatingType.DISLIKE) 1 else 0
        }

        // 获取只包含评分的作业
        var query = Query.query(
            Criteria
                .where("copilotId").`is`(request.id)
                .and("delete").`is`(false)
        )
        // 排除 _id，防止误 save 该不完整作业后原有数据丢失
        query.fields().include("likeCount", "dislikeCount").exclude("_id")
        val copilot = mongoTemplate.findOne(query, Copilot::class.java)
        Assert.notNull(copilot, "作业不存在")

        // 计算评分相关
        var likeCount = copilot!!.likeCount + likeCountChange
        likeCount = if (likeCount < 0) 0 else likeCount
        var ratingCount = likeCount + copilot.dislikeCount + dislikeCountChange
        ratingCount = if (ratingCount < 0) 0 else ratingCount

        val rawRatingLevel = if (ratingCount != 0L) likeCount.toDouble() / ratingCount else 0.0
        val bigDecimal = BigDecimal(rawRatingLevel)
        // 只取一位小数点
        val ratingLevel = bigDecimal.setScale(1, RoundingMode.HALF_UP).toDouble()
        // 更新数据
        query = Query.query(
            Criteria
                .where("copilotId").`is`(request.id)
                .and("delete").`is`(false)
        )
        val update = Update()
        update["likeCount"] = likeCount
        update["dislikeCount"] = ratingCount - likeCount
        update["ratingLevel"] = (ratingLevel * 10).toInt()
        update["ratingRatio"] = ratingLevel
        mongoTemplate.updateFirst(query, update, Copilot::class.java)

        // 记录近期评分变化量前 100 的作业 id
        redisCache.incZSet(
            "rate:hot:copilotIds",
            request.id.toString(),
            1.0, 100, (3600 * 3).toLong()
        )
    }

    /**
     * 将数据库内容转换为前端所需格式 <br></br>
     * 新版评分系统
     */
    private fun formatCopilot(
        copilot: Copilot, ratingType: RatingType?, userName: String,
        commentsCount: Long?
    ): CopilotInfo {
        val info = copilotConverter.toCopilotInfo(
            copilot, userName, copilot.copilotId,
            commentsCount
        )

        info.ratingRatio = copilot.ratingRatio
        info.ratingLevel = copilot.ratingLevel
        if (ratingType != null) {
            info.ratingType = ratingType.display
        }
        // 评分数少于一定数量
        info.notEnoughRating =
            copilot.likeCount + copilot.dislikeCount <= properties.copilot.minValueShowNotEnoughRating

        info.available = true

        // 兼容客户端, 将作业ID替换为数字ID
        copilot.id = copilot.copilotId.toString()
        return info
    }

    fun notificationStatus(userId: String?, copilotId: Long, status: Boolean) {
        val copilotOptional = copilotRepository.findByCopilotId(copilotId)
        Assert.isTrue(copilotOptional != null, "copilot不存在")
        val copilot = copilotOptional!!
        Assert.isTrue(userId == copilot.uploaderId, "您没有权限修改")
        copilot.notification = status
        copilotRepository.save(copilot)
    }

    companion object {
        /*
        首页分页查询缓存配置
        格式为：需要缓存的 orderBy 类型（也就是榜单类型） -> 缓存时间
        （Map.of()返回的是不可变对象，无需担心线程安全问题）
     */
        private val HOME_PAGE_CACHE_CONFIG: Map<String?, Long?> = java.util.Map.of(
            "hot", 3600 * 24L,
            "views", 3600L,
            "id", 300L
        )

        @JvmStatic
        fun getHotScore(copilot: Copilot, lastWeekLike: Long, lastWeekDislike: Long): Double {
            val now = LocalDateTime.now()
            val uploadTime = copilot.uploadTime
            // 基于时间的基础分
            var base = 6.0
            // 相比上传时间过了多少周
            val pastedWeeks = ChronoUnit.WEEKS.between(uploadTime, now) + 1
            base /= ln((pastedWeeks + 1).toDouble())
            // 上一周好评率
            val ups = max(lastWeekLike.toDouble(), 1.0).toLong()
            val downs = max(lastWeekDislike.toDouble(), 0.0).toLong()
            val greatRate = ups.toDouble() / (ups + downs)
            if ((ups + downs) >= 5 && downs >= ups) {
                // 将信赖就差评过多的作业打入地狱
                base *= greatRate
            }
            // 上一周好评率 * (上一周评分数 / 10) * (浏览数 / 10) / 过去的周数
            val s = (greatRate * (copilot.views / 10.0)
                    * max((ups + downs) / 10.0, 1.0)) / pastedWeeks
            val order = ln(max(s, 1.0))
            return order + s / 1000.0 + base
        }
    }
}