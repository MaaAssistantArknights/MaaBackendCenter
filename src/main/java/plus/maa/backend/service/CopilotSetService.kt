package plus.maa.backend.service

import cn.hutool.core.lang.Assert
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import plus.maa.backend.common.utils.IdComponent
import plus.maa.backend.common.utils.converter.CopilotSetConverter
import plus.maa.backend.controller.request.CopilotSetQuery
import plus.maa.backend.controller.request.CopilotSetUpdateReq
import plus.maa.backend.controller.request.copilotset.CopilotSetCreateReq
import plus.maa.backend.controller.request.copilotset.CopilotSetModCopilotsReq
import plus.maa.backend.controller.response.CopilotSetPageRes
import plus.maa.backend.controller.response.CopilotSetRes
import plus.maa.backend.repository.CopilotSetRepository
import plus.maa.backend.repository.UserRepository
import plus.maa.backend.repository.entity.CopilotSet
import plus.maa.backend.repository.entity.MaaUser
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {  }

/**
 * @author dragove
 * create on 2024-01-01
 */
@Service
class CopilotSetService(
    private val idComponent: IdComponent,
    private val converter: CopilotSetConverter,
    private val repository: CopilotSetRepository,
    private val userRepository: UserRepository,
) {

    private val DEFAULT_SORT: Sort = Sort.by("id").descending()

    /**
     * 创建作业集
     *
     * @param req    作业集创建请求
     * @param userId 创建者用户id
     * @return 作业集id
     */
    fun create(req: CopilotSetCreateReq, userId: String?): Long {
        val id = idComponent.getId(CopilotSet.META)
        val newCopilotSet =
            converter.convert(req, id, userId)
        repository.insert(newCopilotSet)
        return id
    }

    /**
     * 往作业集中加入作业id列表
     */
    fun addCopilotIds(req: CopilotSetModCopilotsReq, userId: String) {
        val copilotSet = repository.findById(req.id)
            .orElseThrow { IllegalArgumentException("作业集不存在") }
        Assert.state(copilotSet.creatorId == userId, "您不是该作业集的创建者，无权修改该作业集")
        copilotSet.copilotIds.addAll(req.copilotIds)
        copilotSet.setCopilotIds(copilotSet.distinctIdsAndCheck)
        repository.save(copilotSet)
    }

    /**
     * 往作业集中删除作业id列表
     */
    fun removeCopilotIds(req: CopilotSetModCopilotsReq, userId: String) {
        val copilotSet = repository.findById(req.id)
            .orElseThrow { IllegalArgumentException("作业集不存在") }
        Assert.state(copilotSet.creatorId == userId, "您不是该作业集的创建者，无权修改该作业集")
        val removeIds: Set<Long> = HashSet(req.copilotIds)
        copilotSet.copilotIds.removeIf { o: Long -> removeIds.contains(o) }
        repository.save(copilotSet)
    }

    /**
     * 更新作业集信息
     */
    fun update(req: CopilotSetUpdateReq, userId: String) {
        val copilotSet = repository.findById(req.id)
            .orElseThrow { IllegalArgumentException("作业集不存在") }
        Assert.state(copilotSet.creatorId == userId, "您不是该作业集的创建者，无权修改该作业集")
        copilotSet.setName(req.name)
        copilotSet.setDescription(req.description)
        copilotSet.setStatus(req.status)
        repository.save(copilotSet)
    }

    /**
     * 删除作业集信息（逻辑删除，保留详情接口查询结果）
     *
     * @param id     作业集id
     * @param userId 登陆用户id
     */
    fun delete(id: Long, userId: String) {
        log.info { "delete copilot set for id: $id, userId: $userId" }
        val copilotSet = repository.findById(id)
            .orElseThrow { IllegalArgumentException("作业集不存在") }
        Assert.state(copilotSet.creatorId == userId, "您不是该作业集的创建者，无权删除该作业集")
        copilotSet.setDelete(true)
        copilotSet.setDeleteTime(LocalDateTime.now())
        repository.save(copilotSet)
    }

    fun query(req: CopilotSetQuery): CopilotSetPageRes {
        val pageRequest = PageRequest.of(req.page - 1, req.limit, DEFAULT_SORT)

        val keyword = req.keyword
        val copilotSets = if (StringUtils.isBlank(keyword)) {
            repository.findAll(pageRequest)
        } else {
            repository.findByKeyword(keyword, pageRequest)
        }

        val userIds = copilotSets.stream().map { obj: CopilotSet -> obj.creatorId }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .distinct()
            .toList()
        val userById = userRepository.findByUsersId(userIds)
        return CopilotSetPageRes()
            .setPage(copilotSets.number + 1)
            .setTotal(copilotSets.totalElements)
            .setHasNext(copilotSets.totalPages > req.page)
            .setData(copilotSets.stream().map { cs: CopilotSet ->
                val user = userById.getOrDefault(cs.creatorId, MaaUser.UNKNOWN)
                converter.convert(cs, user.userName)
            }.toList())
    }

    fun get(id: Long): CopilotSetRes {
        return repository.findById(id).map { copilotSet: CopilotSet ->
            val userName = userRepository.findByUserId(copilotSet.creatorId).orElse(MaaUser.UNKNOWN).userName
            converter.convertDetail(copilotSet, userName)
        }.orElseThrow { IllegalArgumentException("作业不存在") }
    }
}
