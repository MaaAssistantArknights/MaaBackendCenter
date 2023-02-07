package plus.maa.backend.config.hint;

import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.lang.Nullable;

import jakarta.servlet.http.HttpServletRequest;


@SpringBootConfiguration
@ImportRuntimeHints(HttpServletRequestRuntimeHint.class)
public class HttpServletRequestRuntimeHint implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        try {
            ProxyHints proxies = hints.proxies();
            proxies.registerJdkProxy(HttpServletRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not register RuntimeHint: " + e.getMessage());
        }
    }

}
