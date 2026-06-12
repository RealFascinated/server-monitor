package cc.fascinated.monitor.web.auth;

import cc.fascinated.monitor.exception.impl.ForbiddenException;
import cc.fascinated.monitor.model.domain.user.UserRole;
import cc.fascinated.monitor.model.persistance.UserRow;
import cc.fascinated.monitor.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthenticatedAdminArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthService authService;

    public AuthenticatedAdminArgumentResolver(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedAdmin.class)
                && UserRow.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String authorization = request != null ? request.getHeader("Authorization") : null;
        UserRow user = this.authService.authenticate(authorization);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
        return user;
    }
}
