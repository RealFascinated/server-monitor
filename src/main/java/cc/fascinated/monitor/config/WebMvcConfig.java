package cc.fascinated.monitor.config;

import cc.fascinated.monitor.web.auth.AuthenticatedAdminArgumentResolver;
import cc.fascinated.monitor.web.auth.AuthenticatedServerArgumentResolver;
import cc.fascinated.monitor.web.auth.AuthenticatedUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AuthenticatedServerArgumentResolver authenticatedServerArgumentResolver;
    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;
    private final AuthenticatedAdminArgumentResolver authenticatedAdminArgumentResolver;

    public WebMvcConfig(AuthenticatedServerArgumentResolver authenticatedServerArgumentResolver,
                        AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver,
                        AuthenticatedAdminArgumentResolver authenticatedAdminArgumentResolver) {
        this.authenticatedServerArgumentResolver = authenticatedServerArgumentResolver;
        this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
        this.authenticatedAdminArgumentResolver = authenticatedAdminArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this.authenticatedServerArgumentResolver);
        resolvers.add(this.authenticatedUserArgumentResolver);
        resolvers.add(this.authenticatedAdminArgumentResolver);
    }
}
