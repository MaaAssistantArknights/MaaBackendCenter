package plus.maa.backend.common.extensions

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.support.PageableExecutionUtils

inline fun <reified T : Any> MongoOperations.findPage(
    pageable: Pageable,
    query: Query = Query(),
    customizer: Query.() -> Unit = {},
): Page<T> {
    val q = query.apply(customizer)
    check(q.skip == 0L && !q.isLimited) { "query should not be paged" }
    val content = find<T>(q.with(pageable))
    val total = count<T>(q.skip(0).limit(0))
    return PageableExecutionUtils.getPage(content, pageable) { total }
}

fun Query.addAndCriteria(vararg criteria: Criteria) {
    addCriteria(Criteria().andOperator(*criteria))
}

fun Query.addOrCriteria(vararg criteria: Criteria) {
    addCriteria(Criteria().orOperator(*criteria))
}

fun Query.addNorCriteria(vararg criteria: Criteria) {
    addCriteria(Criteria().norOperator(*criteria))
}
