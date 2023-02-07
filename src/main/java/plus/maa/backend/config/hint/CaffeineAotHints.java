package plus.maa.backend.config.hint;

import org.springframework.aot.hint.*;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.lang.Nullable;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

@SpringBootConfiguration
@ImportRuntimeHints(CaffeineAotHints.class)
public class CaffeineAotHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        try {
            ReflectionHints reflection = hints.reflection();
            ClassPath.from(classLoader)
                    .getAllClasses()
                    .stream()
                    .filter(it -> it.getPackageName()
                            .startsWith("com.github.benmanes.caffeine.cache") &&
                            it.isTopLevel())
                    .map(ClassInfo::load)
                    .peek(it -> {
                        reflection.registerType(it, MemberCategory.values());
                    }).count();
        } catch (Exception e) {
        }
    }

}
