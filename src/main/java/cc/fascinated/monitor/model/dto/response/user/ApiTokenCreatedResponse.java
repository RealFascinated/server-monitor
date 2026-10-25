package cc.fascinated.monitor.model.dto.response.user;

/** The only time the raw token is ever returned. */
public record ApiTokenCreatedResponse(String token, ApiTokenResponse info) {}
