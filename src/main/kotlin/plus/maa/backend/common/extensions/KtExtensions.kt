package plus.maa.backend.common.extensions

inline fun <T> T?.requireNotNull(lazyMessage: () -> Any): T = requireNotNull(this, lazyMessage)

fun <T> lazySuspend(block: suspend () -> T): suspend () -> T {
    var holder: T? = null
    val access: suspend () -> T = {
        val v = holder ?: block()
        v.also { holder = it }
    }
    return access
}
