package plus.maa.backend.common.utils

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * @Author leaves
 * @Date 2023/1/20 19:14
 */
@Component
@Lazy(false)
class SpringUtil : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        // TODO Auto-generated method stub
        if (Companion.applicationContext == null) {
            Companion.applicationContext = applicationContext
        }
    }

    companion object {
        var applicationContext: ApplicationContext? = null
            private set

        fun getBean(name: String?): Any {
            return applicationContext!!.getBean(name)
        }

        fun <T> getBean(clazz: Class<T>?): T {
            return applicationContext!!.getBean(clazz)
        }

        fun <T> getBean(name: String?, clazz: Class<T>?): T {
            return applicationContext!!.getBean(name, clazz)
        }
    }
}
