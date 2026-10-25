package cc.fascinated.monitor.mcp;

import cc.fascinated.monitor.mcp.auth.McpAuth;
import cc.fascinated.monitor.mcp.auth.McpAuthFilter;
import cc.fascinated.monitor.service.ApiTokenService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Replaces Spring AI's stateless transport so every tool call carries the authenticated API token. Stateless is
 * deliberate: the monitor needs no sampling or elicitation, and tools get their identity from the transport context
 * instead of thread-locals, which the SDK does not preserve across its scheduler hops.
 */
@Configuration
public class McpConfig {

    @Bean
    public WebMvcStatelessServerTransport webMvcStatelessServerTransport(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties properties
    ) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint(properties.getMcpEndpoint())
                .contextExtractor(request -> request.attribute(McpAuthFilter.PRINCIPAL_ATTRIBUTE)
                        .<McpTransportContext>map(principal -> McpTransportContext.create(
                                Map.of(McpAuth.PRINCIPAL_KEY, principal)))
                        .orElse(McpTransportContext.EMPTY))
                .build();
    }

    @Bean
    public FilterRegistrationBean<McpAuthFilter> mcpAuthFilter(ApiTokenService apiTokenService,
                                                               McpServerStreamableHttpProperties properties) {
        FilterRegistrationBean<McpAuthFilter> registration = new FilterRegistrationBean<>(
                new McpAuthFilter(apiTokenService));
        registration.addUrlPatterns(properties.getMcpEndpoint(), properties.getMcpEndpoint() + "/*");
        return registration;
    }
}
