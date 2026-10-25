package cc.fascinated.monitor.mcp.auth;

import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.user.ApiTokenPrincipal;
import cc.fascinated.monitor.service.ApiTokenService;
import cc.fascinated.monitor.util.AuthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Requires a valid API token on the MCP endpoint and exposes it to the transport as a request attribute. */
public class McpAuthFilter extends OncePerRequestFilter {
    public static final String PRINCIPAL_ATTRIBUTE = McpAuthFilter.class.getName() + ".principal";

    private final ApiTokenService apiTokenService;

    public McpAuthFilter(ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // CORS preflights carry no credentials; CorsConfig answers them.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        ApiTokenPrincipal principal;
        try {
            principal = this.apiTokenService.authenticate(
                    AuthUtils.extractBearerValue(request.getHeader("Authorization")));
        } catch (UnauthorizedException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"%s\"}".formatted(ex.getMessage().replace("\"", "'")));
            return;
        }
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        chain.doFilter(request, response);
    }
}
