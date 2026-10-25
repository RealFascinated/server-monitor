package cc.fascinated.monitor.mcp.auth;

import cc.fascinated.monitor.model.domain.user.ApiTokenPrincipal;
import cc.fascinated.monitor.model.domain.user.ApiTokenScope;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServer;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.customizer.McpStatelessSyncServerCustomizer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Hides tools from {@code tools/list} when the caller's token lacks their scope, so a client only sees what it can
 * use. Calling a hidden tool is still refused by the tool itself.
 */
@Component
public class McpToolVisibility implements McpStatelessSyncServerCustomizer {
    private final Map<String, ApiTokenScope> scopesByTool = new HashMap<>();

    /** @throws IllegalStateException when a tool has no {@link RequiresScope}, so a new tool cannot ship without one */
    public McpToolVisibility(ListableBeanFactory beans) {
        String toolPackage = McpToolVisibility.class.getPackageName().replaceFirst("\\.auth$", ".tool");
        for (String beanName : beans.getBeanNamesForType(Object.class)) {
            Class<?> type = beans.getType(beanName);
            if (type == null || !type.getName().startsWith(toolPackage)) {
                continue;
            }
            ReflectionUtils.doWithMethods(type, method -> {
                McpTool tool = AnnotatedElementUtils.findMergedAnnotation(method, McpTool.class);
                if (tool == null) {
                    return;
                }
                RequiresScope scope = AnnotatedElementUtils.findMergedAnnotation(method, RequiresScope.class);
                if (scope == null) {
                    throw new IllegalStateException("MCP tool %s must declare @RequiresScope".formatted(tool.name()));
                }
                this.scopesByTool.put(tool.name(), scope.value());
            });
        }
    }

    @Override
    public void customize(McpServer.StatelessSyncSpecification specification) {
        specification.addToolFilter((context, tool) -> isVisible(context, tool.name()));
    }

    boolean isVisible(McpTransportContext context, String toolName) {
        ApiTokenScope scope = this.scopesByTool.get(toolName);
        return scope != null
                && context != null
                && context.get(McpAuth.PRINCIPAL_KEY) instanceof ApiTokenPrincipal principal
                && principal.has(scope);
    }
}
