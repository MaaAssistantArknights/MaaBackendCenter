package plus.maa.backend.common.utils

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

@Suppress("unused")
object PinyinUtil {
    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    fun pinyinFull(input: String): String {
        val sb = StringBuilder()
        input.forEach { ch ->
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, format)
            if (!pinyinArray.isNullOrEmpty()) {
                sb.append(pinyinArray[0])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun pinyinAbbr(input: String): String {
        val sb = StringBuilder()
        input.forEach { ch ->
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                sb.append(pinyinArray[0][0])
            } else {
                sb.append(pinyinArray?.firstOrNull()?.get(0) ?: ch)
            }
        }
        return sb.toString()
    }
}
