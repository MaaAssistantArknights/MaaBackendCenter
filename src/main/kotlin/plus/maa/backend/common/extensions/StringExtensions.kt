package plus.maa.backend.common.extensions

fun String.removeQuotes() = replace("[\"“”]".toRegex(), "")

fun String.blankAsNull() = if (isBlank()) null else this
