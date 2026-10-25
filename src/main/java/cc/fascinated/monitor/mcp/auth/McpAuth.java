package cc.fascinated.monitor.mcp.auth;

import cc.fascinated.monitor.exception.impl.UnauthorizedException;
import cc.fascinated.monitor.model.domain.user.ApiTokenPrincipal;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import cc.fascinated.monitor.model.persistance.UserRow;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller of an MCP tool. {@link McpAuthFilter} authenticates the HTTP request and the transport copies
 * the principal into the {@link McpTransportContext} handed to each tool, so nothing depends on the request thread.
 */
@Component
public class McpAuth {
    public static final String PRINCIPAL_KEY = "monitor.apiTokenPrincipal";

    /**
     * Every tool calls this first: it proves the token holds {@code scope} and returns the user to act as.
     *
     * @throws cc.fascinated.monitor.exception.impl.ForbiddenException naming the missing scope
     */
    public UserRow require(McpTransportContext context, ApiTokenScope scope) {
        return principal(context).require(scope);
    }

    public ApiTokenPrincipal principal(McpTransportContext context) {
        if (context != null && context.get(PRINCIPAL_KEY) instanceof ApiTokenPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("Missing API token");
    }
}
