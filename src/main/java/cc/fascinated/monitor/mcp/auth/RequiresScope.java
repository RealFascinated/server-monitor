package cc.fascinated.monitor.mcp.auth;

import cc.fascinated.monitor.model.domain.user.ApiTokenScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The scope a tool needs. {@link McpToolVisibility} uses it to leave tools out of {@code tools/list} for tokens that
 * could not run them. It does not enforce anything: the tool still calls {@link McpAuth#require} with the same scope.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresScope {
    ApiTokenScope value();
}
