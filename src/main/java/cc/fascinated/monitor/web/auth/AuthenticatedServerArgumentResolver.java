package cc.fascinated.monitor.web.auth;

import cc.fascinated.monitor.model.persistance.ServerRow;
import cc.fascinated.monitor.service.ServerService;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticatedServerArgumentResolver implements HandlerMethodArgumentResolver {
    private final ServerService serverService;

    public AuthenticatedServerArgumentResolver(ServerService serverService) {
        this.serverService = serverService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedServer.class) && ServerRow.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String authorization = request != null ? request.getHeader("Authorization") : null;
        return this.serverService.authenticateIngestRequest(authorization);
    }
}
