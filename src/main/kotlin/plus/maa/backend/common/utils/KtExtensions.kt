package plus.maa.backend.common.utils


inline fun <T> T?.requireNotNull(lazyMessage: () -> Any): T = requireNotNull(this, lazyMessage)