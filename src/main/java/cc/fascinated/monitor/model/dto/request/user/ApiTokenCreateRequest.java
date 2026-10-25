package cc.fascinated.monitor.model.dto.request.user;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Validated in {@code ApiTokenService}: the app has no handler that turns bean-validation failures into a 400. */
public record ApiTokenCreateRequest(
        String name,
        List<String> scopes,
        /** Days until the token stops working (1-3650); null means it never expires. */
        @Nullable Integer expiresInDays
) {}
