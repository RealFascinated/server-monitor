package cc.fascinated.monitor.model.domain.user;

import cc.fascinated.monitor.exception.impl.ForbiddenException;
import cc.fascinated.monitor.model.persistance.UserRow;

import java.util.Set;

/** An authenticated API token: the user it acts as and what it is allowed to do. */
public record ApiTokenPrincipal(UserRow user, Set<ApiTokenScope> scopes) {

    public boolean has(ApiTokenScope scope) {
        return scope.isGrantedBy(this.scopes);
    }

    /** @throws ForbiddenException naming the missing scope */
    public UserRow require(ApiTokenScope scope) {
        if (!has(scope)) {
            throw new ForbiddenException(("This API token is missing the \"%s\" scope. Ask the user to create a token "
                    + "that has it under Account settings, API tokens").formatted(scope.id()));
        }
        return this.user;
    }
}
