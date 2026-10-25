# MCP server with scoped API tokens

## Context

Expose the monitor's data and a small set of safe actions to MCP clients (Claude Code, Cursor, Claude Desktop) so a model can answer "why was box X slow at 3am?", "what's firing?", or "tune my CPU alert". The MCP server lives **inside the API** as a Spring AI MCP server, calling the same services the controllers use. Nothing is added as a separate deployable.

Every service the tools need already takes a `UserRow` and enforces access itself (`ServerService.requireServer`, `ServerAccessService.requirePermission`, `AlertRuleService` ownership checks). The tools are therefore thin adapters, and a token can never see more than its user can.

Decisions already made: Spring AI, **one tool per class**, scoped auth, **no rate limiting** for now.

## Non-goals

- OAuth 2.1 / dynamic client registration. Clients send a static `Authorization: Bearer` header, which Claude Code and Cursor both support.
- Rate limiting.
- Tools for server create/delete, ingest-token rotation, members/invites, passwords, sessions, admin, or the agent `/ingest` and `/heartbeat` endpoints.
- Creating/editing/deleting alert destinations (they hold webhook secrets) and creating/deleting alert groups. Revisit later.
- MCP resources/prompts. Tools only for v1.

## 1. Dependency and config

Add `spring-ai-starter-mcp-server-webmvc` via the Spring AI BOM in [build.gradle.kts](build.gradle.kts).

**Resolved:** Spring AI 2.0.2 (BOM) works with Spring Boot 4.1.2 and supports Streamable HTTP and `STATELESS`.

```yaml
spring:
  ai:
    mcp:
      server:
        name: server-monitor
        protocol: STATELESS        # see "Caller identity" below
        annotation-scanner: { enabled: true }
```

`STATELESS` is preferred because it needs no sampling/elicitation and keeps tool execution on the request thread. Confirm the endpoint path (default `/mcp`) and that the property names match the pinned version.

## 2. Auth: API tokens

Existing session tokens (`user_sessions`) must **not** work for MCP, and API tokens must not work for the REST API. They are separate credentials with separate tables.

### Migration `V14__api_tokens.sql`

```sql
CREATE TABLE api_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name          VARCHAR(64)  NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,   -- AuthUtils.hash(token)
    token_prefix  VARCHAR(12)  NOT NULL,          -- shown in the UI to tell tokens apart
    scopes        VARCHAR(255) NOT NULL,          -- comma-separated ApiTokenScope names
    created_at    TIMESTAMPTZ  NOT NULL,
    expires_at    TIMESTAMPTZ,                    -- null = never
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ
);
CREATE INDEX api_tokens_user_idx ON api_tokens(user_id);
```

(Match the `users` table/column naming and timestamp type used in V1/V6 when writing the real migration.)

### Scopes: `model/domain/ApiTokenScope`

| Scope | Grants |
|---|---|
| `SERVERS_READ` (`servers:read`) | list servers, status, incidents |
| `METRICS_READ` (`metrics:read`) | metric catalog, series, range queries |
| `ALERTS_READ` (`alerts:read`) | rules, groups, destinations (masked), events, preview |
| `ALERTS_WRITE` (`alerts:write`) | silence/unsilence, enable/disable, create/update/delete rules, group membership, test destination |

`ALERTS_WRITE` implies `ALERTS_READ` through `ApiTokenScope.implies(...)`, since you can't edit rules you can't read. A token with no scopes is rejected at creation.

### Code

- `model/persistance/ApiTokenRow`, `repository/ApiTokenRepository` (`findByTokenHash`, `findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc`).
- `service/ApiTokenService`:
  - `create(user, name, scopes, expiresAt)`: generates `smk_` + 32 random bytes base64url (same `SecureRandom` approach as `SessionService`), stores `AuthUtils.hash`, returns the raw token **once**.
  - `list(user)`, `revoke(user, id)` (own tokens only).
  - `authenticate(rawToken)` returns `ApiTokenPrincipal(UserRow user, Set<ApiTokenScope> scopes)`; rejects unknown, revoked, or expired. Updates `last_used_at` at most once per minute per token to avoid a write on every call.
  - Scheduled cleanup of tokens expired > 30 days, modelled on `SessionService.deleteExpiredSessions`.
- `controller/v1/ApiTokenController` at `/v1/user/api-tokens` (`GET`, `POST`, `DELETE /{id}`) using `@AuthenticatedUser`, so it accepts **session auth only**. An API token can never mint or revoke tokens.
- `mcp/auth/McpAuthFilter` (`OncePerRequestFilter` registered for `/mcp/**` only): extracts the bearer value with `AuthUtils.extractBearerValue`, calls `ApiTokenService.authenticate`, stores the principal as a request attribute, and answers `401` with `WWW-Authenticate: Bearer` on failure. It is registered via a `FilterRegistrationBean`; there is no Spring Security in this project and none is added.

### Caller identity and scope checks

`mcp/auth/McpAuth.require(McpTransportContext, ApiTokenScope)` returns the `UserRow` or throws `ForbiddenException` naming the missing scope.

**As built (replaces the planned thread-local lookup):** `McpAuthFilter` stores the principal as a request attribute and a custom `WebMvcStatelessServerTransport` (`McpConfig`) has a `contextExtractor` that copies it into the `McpTransportContext`. Every `@McpTool` method takes that context as its first parameter and calls `McpAuth.require(...)` first. A `RequestContextHolder`/thread-local lookup was rejected because the MCP SDK hops schedulers and would lose it. `McpToolScopeCoverageTest` calls every tool with an under-scoped token, so a tool without a scope check fails the build.

### Errors

`ExceptionControllerAdvice` does not apply to MCP. Verified by tests: exceptions thrown from a tool method (`NotFoundException`, `ForbiddenException`, `BadRequestException`) reach the client as `isError: true` with the original message, so no handler was needed.

### Output format (as built)

Tools return `String` produced by `McpJson.write`, a mapper with ISO dates and no null fields. Spring AI's default converter writes every null, which wastes the model's context.

### Metric ids (as built)

`list_metrics` is the single discovery tool. The queryable set is `VmMetricCatalog.allGaugeNames()` plus the `ServerStatusSeries` names (so `monitor_server_up` is included); `alertable` flags the subset usable in alert rules. With a `serverId` it returns only what that server reports, with label values for labelled metrics (one instant query). `query_metrics` validates the metric and label keys against the same set.

### Other deviations

- Request validation is explicit in `ApiTokenService` (name 1-64 chars, at least one scope, expiry 1-3650 days, 20 active tokens per user) because the app has no bean-validation handler (those failures return 500).
- `last_used_at` is throttled on the row's own value (once per minute) rather than an in-memory map.
- `query_metrics` anchors its step grid on `end`, so the last point is the newest sample.

## 3. Tools: one class per tool

Package `cc.fascinated.monitor.mcp.tool`, class names end in `Tool`. Each is a `@Component` with exactly one `@McpTool(name = ..., description = ...)` method, parameters documented with `@McpToolParam`, and constructor-injected services. Descriptions are written for the model: say what is returned and when to use the tool.

### Phase 1: read-only

| Class | Scope | Calls | Notes |
|---|---|---|---|
| `ListServersTool` | servers:read | `ServerService.listServers` | `ServerResponse`: status, last heartbeat, agent version, 30d uptime, inventory, CPU/memory/disk snapshot, folder. Optional `serverId` filter, so there is no separate get-server or get-status tool (`ServerStatusResponse` is a subset of this). |
| `ListServerIncidentsTool` | servers:read | `ServerService.listIncidents` | paged (`page`, `count`) |
| `ListMetricsTool` | metrics:read | new `McpMetricCatalogService` over `VmMetricCatalog.allGaugeNames()` | the **only** metric-discovery tool: every queryable metric id, optionally with what a server is reporting. See below. |
| `QueryMetricsTool` | metrics:read | new `McpMetricQueryService` | see below |
| `ListAlertEventsTool` | alerts:read | `AlertHistoryService.list` | filters: rule, group, server, state (`firing` covers "what's firing now"); paged |
| `ListAlertGroupsTool` | alerts:read | `AlertGroupService` | optional `groupId` filter; includes member `serverIds` and destination |
| `ListAlertRulesTool` | alerts:read | `AlertRuleService` | optional `groupId` / `ruleId` filters |
| `ListAlertDestinationsTool` | alerts:read | `AlertDestinationService` | webhook URL is already masked in the response |
| `PreviewAlertRuleTool` | alerts:read | `AlertPreviewService.preview` | dry-run a rule against real history |

**One tool per concept.** Where a "get one" or "filtered" variant would only repeat a list tool, the list tool takes an optional filter instead of a second tool.

**`ListMetricsTool`: the single tool for queryable metrics.** `VmMetricCatalog` has two sets: `allGaugeNames()` (everything stored and queryable) and `alertable()` (a subset users can write rules on). `AlertSeriesService.metrics()` and `.series()` only know the alertable subset and `series()` rejects anything else, so reusing them as-is would hide metrics that `QueryMetricsTool` can read (for example SMART power-on hours). `ListMetricsTool` replaces both with one call and one entry per queryable metric:

```json
{ "metric": "monitor_smart_power_on_hours", "group": "smartDevices", "labelKeys": ["device"],
  "alertable": false,
  "series": [{ "device": "sda" }, { "device": "sdb" }] }
```

- `metric` is the id passed to `QueryMetricsTool` and the rule tools.
- `group` comes from `MetricFamily.section().jsonKey()`, `labelKeys` from the family's `identityLabels`.
- `displayName`, `unit`, `suggestedOperator` and `suggestedThreshold` are filled from `alertInfo()` when the metric is alertable. Units for other metrics are not known to the backend (they live in `www/src/lib/metrics/sections/server/charts.ts`), so they are omitted (nulls are dropped) and the tool description explains the id-suffix conventions instead.
- Optional `group` filter (e.g. `containers`, `zfsPools`).
- Optional `serverId`. Without it the result is the catalog only and `series` is omitted. With it (after a `VIEW_METRICS` check), the result is limited to metrics that server is actually reporting, and `series` lists the identity-label sets it has (which disks, interfaces, containers, GPUs exist). This is one VictoriaMetrics query using `VmMetricCatalog.selectorForServer(serverId)`, grouped by metric name with `SeriesKeys.seriesLabels` and `identityLabelsOf`, instead of one query per metric. Check the lookback window on that query so it reflects what is reported now, not everything ever stored.
- `CreateAlertRuleTool` and `UpdateAlertRuleTool` keep validating against `alertable()`; their descriptions point at the `alertable` flag.

**`QueryMetricsTool` / `McpMetricQueryService`.** This is the one tool with no ready-made service call. Inputs: `serverId`, `metric`, optional label filter map, `from`, `to` (ISO-8601), optional `step`. The service:

1. checks `VIEW_METRICS` on the server via `ServerAccessService.requirePermission`;
2. validates the metric against `VmMetricCatalog.allGaugeNames()` (the full queryable set, **not** `alertable()`, which is a subset) and label keys against its `identityLabels`;
3. builds the query through `PromqlQueryBuilder` / `VictoriaMetricsQueryClient`, reusing what `AlertPreviewService` already does;
4. picks a step so each series returns at most ~200 points, and returns a compact `{labels, unit, points:[[epochSec, value]...]}` plus min/max/avg per series.

The point cap is only output shaping so a day of 1s data doesn't flood the model's context. It is not rate limiting.

Return existing response records where they are already compact. Add small MCP-specific DTOs under `mcp/dto/` only where the existing response is large or UI-shaped.

### Phase 2: writes (`alerts:write`)

| Class | Calls |
|---|---|
| `SetAlertEventSilencedTool` | `AlertHistoryService.silence` / `unsilence`, chosen by a `silenced` boolean |
| `CreateAlertRuleTool` | `AlertRuleService.create` (same validation as the REST path) |
| `UpdateAlertRuleTool` | `AlertRuleService.update`; takes `ruleId` plus only the fields to change, merged onto the current rule. Covers enable/disable too (`AlertRuleRequest.enabled`), so there is no separate enable tool. |
| `DeleteAlertRuleTool` | `AlertRuleService.delete` |
| `SetAlertGroupServersTool` | read-modify-write on `AlertGroupService.update` (`AlertGroupRequest.serverIds`); takes `groupId` with `add` and `remove` server id lists |
| `TestAlertDestinationTool` | `AlertDestinationService` test (posts a real Discord message) |

Rules follow the alert-groups design: they belong to a group, never to a server. `CreateAlertRuleTool` takes a `groupId`, not a `serverId`. Per-server exclusion is "leave the server out of the group". Keep the `@Modifying(clearAutomatically = true)` detach pitfall in mind for the group membership tools: they must not mix those queries with group entity access in the same transaction.

Write tool descriptions should tell the model to call `PreviewAlertRuleTool` before creating or changing a rule, and the destructive ones (`DeleteAlertRuleTool`) should say they are irreversible.

## 4. Frontend (`www`)

Add an "API tokens" section next to `components/user/user-sessions-section.tsx` in `account-settings-view.tsx`, following the pattern of `lib/api/user/sessions.ts` / `sessions.queries.ts`:

- `lib/api/user/api-tokens.ts` + `api-tokens.queries.ts` (list, create, revoke).
- `components/user/api-tokens-section.tsx`: table (name, prefix, scopes, created, last used, expires), create dialog (name, scope checkboxes, expiry: 30d / 90d / 1y / never), and a one-time reveal of the raw token plus a copy-pasteable MCP client config snippet, e.g. `claude mcp add --transport http server-monitor <api-url>/mcp --header "Authorization: Bearer <token>"`.
- Follow `.cursor/rules/monitor-design-system.mdc` and `typescript-react.mdc`.

## 5. Tests

Integration tests on `IntegrationTestBase` / `TestAuthSupport` (Testcontainers):

- `ApiTokenServiceTest`: create returns the raw token once, hash stored, revoked and expired tokens rejected, scopes persisted, `last_used_at` throttled.
- `ApiTokenControllerIntegrationTest`: create/list/revoke; an API token and an anonymous call both get 401 on this endpoint.
- `McpAuthFilterTest`: no header, bad token, session token (must be rejected), revoked, expired → 401; valid → passes.
- MCP tool tests via the MCP endpoint (or by invoking the tool beans with a populated request context):
  - a missing scope fails with an error naming the scope;
  - user A's token cannot read user B's server, rule or group;
  - `ALERTS_WRITE` implies `ALERTS_READ`;
  - `QueryMetricsTool` rejects an unknown metric or label and caps the number of points;
  - write tools mutate through the same validation as REST (an invalid rule is rejected).
- A test that asserts every `@McpTool` class calls `McpAuth.require` (e.g. scan beans for `@McpTool` methods and check each declares a scope in a small registry), so a new tool can't ship without a scope check.

## 6. Order of work

1. Verify the Spring AI / Boot 4.1 version; add dependency and config; confirm `/mcp` starts and the request-thread / error-handling spike (section 2).
2. `V14` migration, `ApiTokenScope`, row/repo, `ApiTokenService`, `ApiTokenController`, tests.
3. `McpAuthFilter` + `McpAuth`, tests.
4. Phase 1 tools, starting with `ListServersTool` end to end (verify with a real client), then the rest; `McpMetricQueryService` + `QueryMetricsTool`.
5. Frontend token management.
6. Phase 2 write tools, each with a test.
7. README: document the endpoint, token creation, scopes, and a client config example.

Phases 1–3 are shippable on their own: they give authenticated, read-only access.

## Risks / open points

- **Static bearer tokens** mean a leaked token is valid until revoked or expired, so the UI defaults to an expiry and shows `last_used_at`.
- **Write tools act as the user**, including notifying real Discord channels (`TestAlertDestinationTool`, and any rule that then fires). Keep them behind `alerts:write`, which is never selected by default in the UI.
- **Fresh data lags by ~30s** because VictoriaMetrics hides the newest samples (`-search.latencyOffset`); tests backfill older samples instead of ingesting "now".
