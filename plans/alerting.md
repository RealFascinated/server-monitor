# Alerting + Discord webhook notifications

## Context

The API stores metrics in VictoriaMetrics and only has one alert-like feature: the `@Scheduled` stale-heartbeat sweep (`ServerService.markStaleServersOffline`) that opens an `IncidentRow`. There are no user-defined rules and no notifications.

Goal: users define their own alert rules, **per server** or **account-wide**, on **metrics the platform already collects**; rules are evaluated on the ingest hot path and a Discord webhook is notified on fire/resolve. Rules are user data (no per-metric enum); operators and destinations are enums so more can be added later. The `www` frontend gets a full alert builder (templates, metric/series pickers, live preview of the rule against real history), rule and destination management, history, and a per-server Alerts tab.

## Audit against existing Linux server monitors

Compared with Netdata (health alarms, verified in its docs), Beszel, Uptime Kuma (notification model, partly verified) and Zabbix (from memory). Beszel/Zabbix details below are from memory, not checked this session.

| Concept in other apps | Who | Verdict in this plan |
|---|---|---|
| Alert = metric + aggregation over a time window + threshold. Netdata `lookup: average -5m`; Beszel "average over last N min". Averaging, not "every sample must breach", is the norm for spiky metrics like CPU. | Netdata, Beszel | **Adopt.** Earlier draft used `for_seconds` (all samples breach); replaced by `window_seconds` with average semantics (0 = instantaneous). |
| Hysteresis: enter threshold differs from leave threshold (Netdata status-conditional thresholds, Zabbix recovery expression). | Netdata, Zabbix | **Adopt**, optional `recovery_threshold`, defaults to `threshold`. |
| Repeat notification while still bad (`repeat:` in Netdata, "resend every N" in Kuma). | Netdata, Kuma | **Adopt**, optional `repeat_interval_seconds`. |
| Severity levels (WARNING / CRITICAL). | Netdata | **Adopt minimally**: `severity` on the rule (colour/label only); two thresholds on one rule is skipped, users make two rules. |
| Template vs instance: one alert applies to every disk / interface; a specific alarm overrides a template with the same name. | Netdata | **Adopt.** Labelled series already give per-disk/interface instances; a server-scoped rule **overrides** the user's account-wide rule for the same metric+labels (even when disabled = per-server mute). The earlier draft would have double-notified. |
| Global default with per-system override ("all systems" in Beszel; Kuma "apply on all existing monitors" / "default enabled"). | Beszel, Kuma | **Covered** by account-wide scope + override, plus client-side preset templates in the builder (CPU > 90% for 10m, memory, disk, temperature, offline), like Netdata's shipped defaults. |
| Notification channels are separate objects attached to alerts/monitors; roles route to methods. | Kuma, Netdata, Beszel (Shoutrrr URLs) | **Covered**: `alert_destinations` selected per rule. |
| Fixed, curated metric menu with units. Netdata alerts live on charts with units, Beszel offers a fixed metric list. | all | **Adopt as metadata only**: rules stay data, but a small `AlertMetricCatalog` gives label + unit for known metrics (message formatting and UI picker). The backend has no unit info today (units live in `www/src/lib/metrics/sections/server/charts.ts`). |
| "Server down" is a first-class alert with a delay before notifying. | Beszel, Kuma, Netdata | **Adopt** via `monitor_server_up`, evaluated by the existing offline sweep on every tick so `window_seconds`/repeat apply. |
| Recovery message with previous state and duration, link to the host. | Netdata | **Adopt** (duration + link built from `monitor.website-url`). |
| Test-notification button. | all | **Covered.** |
| Rule builder with a live preview of the metric against the threshold and an evaluation-behaviour section (pending period, keep firing). | Grafana (builder UX; pending/keep-firing verified in its docs) | **Adopt** in the frontend builder: threshold line on the real history chart with shaded firing periods, driven by the backend preview endpoint. Beszel/Kuma-style presets keep the common path to a few clicks. |
| Alert history log. | Netdata, Zabbix | **Covered**: `alert_events`. |
| Notification storm control: `delay` (Netdata), grouping (Alertmanager). | Netdata | **Adopt minimally**: per-destination coalescing window that batches events into one Discord message. |
| Escalation chains, on-call, dependency/parent suppression, silences. | Zabbix, Netdata | **Skip** for v1. |
| Many integrations through a library (Beszel uses Shoutrrr, Go). | Beszel | **Not applicable** in Java; only Discord now, `AlertDestination` enum is the extension point. |

Evaluating at ingest rather than polling a TSDB matches Zabbix (triggers evaluate on data receipt) and Netdata (evaluates next to the data); it needs no extra infrastructure.

## Design decisions

- **Rules are data.** A rule stores metric name, optional label filter (`{"disk":"/"}`), `AlertOperator`, threshold, optional recovery threshold, `window_seconds`, severity, optional repeat interval. Metric name is validated against `VmMetricCatalog` plus `monitor_server_up`; label keys against that metric family's `identityLabels` in `MetricFamily`.
- **Evaluate at the existing chokepoint.** Every series is written through `MetricWriteContext.gauge(name, value)` with labels already merged. Add an optional `MetricSink` there; live ingest passes one that feeds `AlertEvaluator`.
- **Zero cost when unused.** `sinkFor(serverId, ts)` returns `null` when the server has no applicable rules; otherwise one `HashMap` lookup by metric name per gauge, work only on matches.
- **Live samples only.** `ingestBatch()` (backfill) passes no sink, matching its rule that backfill must not touch status/incidents.
- **No I/O on the ingest thread.** Transitions go on a bounded queue; a virtual-thread worker persists events and posts to Discord.
- **Ownership.** Each rule belongs to a user and uses that user's destination. `server_id` set = one server; `server_id IS NULL` = every server the user is a member of (`server_members`). Server-scoped rules need `VIEW_METRICS` (a viewer can set personal alerts), so no new `ServerPermission`s. Rules are private to their creator.
- **Secrets.** Webhook URLs encrypted with `EncryptionService`, validated as `https://discord.com|discordapp.com/api/webhooks/...` (SSRF guard), never returned by the API (masked).

## Enums (`model/domain/alert/`)

```java
public enum AlertOperator {
    LESS_THAN("below", "back above", (v, t) -> v < t),
    MORE_THAN("above", "back below", (v, t) -> v > t);
    // fields: phrase, recoveryPhrase, DoubleBinaryPredicate test
    public boolean test(double value, double threshold);
}

public enum AlertSeverity { WARNING, CRITICAL }   // embed colour + label

public enum AlertDestination {
    DISCORD_WEBHOOK((alert, target) -> target.postJson(DiscordMessages.payload(alert)));

    private final AlertSender sender;
    public void send(AlertNotification alert, AlertDestinationTarget target) { sender.send(alert, target); }
    public void validateConfig(String config);   // Discord: URL shape + host allowlist
}
@FunctionalInterface interface AlertSender { void send(AlertNotification alert, AlertDestinationTarget target); }
```

- `AlertDestinationTarget`: decrypted config + shared `HttpClient`; `postJson(String)` is the only I/O the enum lambda sees, so constants need no Spring beans.
- `AlertNotification` (record): rule name, server name + id, metric, series labels, operator, threshold, value, severity, transition (`FIRING`/`RESOLVED`/`REPEAT`), firing duration (resolved), occurredAt.
- `AlertMetricCatalog` (`@UtilityClass`): `metric name → (label, AlertUnit)`; `AlertUnit` ∈ PERCENT, BYTES, BYTES_PER_SECOND, MILLISECONDS, CELSIUS, WATTS, COUNT, NONE with `format(double)`. Covers the curated set first (CPU, memory, swap, load, iowait, disk usage %, disk IO util, temperatures, GPU util/temp, fd usage, battery, container CPU/mem, server up); unknown metrics fall back to the raw name and a plain number.
- Discord message, as one embed (colour by severity) whose description matches the sketch:
  ```
  **web-01**'s CPU usage is above 90%
  CPU usage: 96.55% (avg 5m) · disk: /      <- labels only if present
  ```
  Resolved: `**web-01**'s CPU usage is back below 90% — now 42.10%, was firing for 12m`. Title links to the server page. Always `allowed_mentions: {"parse": []}`; escape markdown in server names so a name can't ping `@everyone`.
- Verify `cpu_usage`/`mem_usage` units against the agent (assumed 0–100 %).

## Schema: `V8__alerting.sql`

```sql
CREATE TABLE alert_destinations (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  type TEXT NOT NULL,             -- AlertDestination
  config_encrypted TEXT NOT NULL, -- AES-GCM via EncryptionService (webhook URL)
  created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE alert_rules (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  server_id BIGINT REFERENCES servers(id) ON DELETE CASCADE,  -- NULL = account-wide
  destination_id BIGINT NOT NULL REFERENCES alert_destinations(id) ON DELETE CASCADE,
  name TEXT,
  metric TEXT NOT NULL,           -- metric name from VmMetricCatalog (or monitor_server_up)
  label_filter JSONB NOT NULL DEFAULT '{}',
  operator TEXT NOT NULL,         -- AlertOperator
  threshold DOUBLE PRECISION NOT NULL,
  recovery_threshold DOUBLE PRECISION,           -- hysteresis; NULL = threshold
  window_seconds INT NOT NULL DEFAULT 0,         -- average over this window; 0 = instantaneous
  severity TEXT NOT NULL DEFAULT 'WARNING',
  repeat_interval_seconds INT,                   -- NULL = no repeat
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX alert_rules_user_id_idx ON alert_rules(user_id);
CREATE INDEX alert_rules_server_id_idx ON alert_rules(server_id);
CREATE TABLE alert_events (
  id BIGSERIAL PRIMARY KEY,
  rule_id BIGINT NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
  server_id BIGINT NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
  series_key TEXT NOT NULL,       -- canonical label set, '' for unlabelled
  started_at TIMESTAMPTZ NOT NULL,
  resolved_at TIMESTAMPTZ,
  resolve_reason TEXT,            -- RECOVERED | SERIES_GONE | RULE_CHANGED
  peak_value DOUBLE PRECISION NOT NULL,
  last_notified_at TIMESTAMPTZ,
  notify_error TEXT
);
CREATE INDEX alert_events_rule_started_idx ON alert_events(rule_id, started_at DESC);
CREATE INDEX alert_events_open_idx ON alert_events(rule_id, server_id) WHERE resolved_at IS NULL;
```

Entities `AlertDestinationRow`, `AlertRuleRow`, `AlertEventRow` in `model/persistance/` (Lombok style as `IncidentRow`, `@Enumerated(EnumType.STRING)`), repositories in `repository/`. `label_filter` as a JSON `Map<String,String>` column (same approach `SettingRow` uses for JSONB).

## Components (new package `cc.fascinated.monitor.alert`)

| Class | Responsibility |
|---|---|
| `AlertRuleCache` | Per-server immutable snapshot `Map<String metricName, RuleSnapshot[]>`. One query loads enabled+disabled rules where `server_id = :s` or (`server_id IS NULL` and user is a member of `:s`). **Override pass:** for each (user, metric, label_filter) a server-scoped rule suppresses the account-wide one (a disabled server-scoped rule suppresses and is itself dropped). Empty snapshots cached too. `invalidate(serverId)` / `invalidateAll()` on rule, destination or membership change, plus short TTL backstop. |
| `AlertEvaluator` | State machine lives in a pure `AlertRuleEngine` (no Spring/I/O) that both the live sink and the preview endpoint drive. `sinkFor(serverId, ts)` → `MetricSink` or `null`. Per matched series: label filter check, update the series' window, then state `OK`/`FIRING` in `ConcurrentHashMap<(ruleId, seriesKey), AlertState>`. Window 0: value as-is. Window >0: average of the last `window_seconds` from a small bucket ring (10 buckets of `(sum,count)`, allocated only for windowed rules); evaluate only once the window is covered (first sample at least ~90% of the window old), so a fresh state can't fire on one sample. OK→FIRING when `operator.test(effective, threshold)`; FIRING→OK when `!operator.test(effective, recovery ?? threshold)`. Emits FIRING / RESOLVED. |
| `AlertStateSweeper` | `@Scheduled` on the existing `refreshIntervalMs`: (1) evaluates `monitor_server_up = 0` for OFFLINE servers every tick (the offline sweep currently writes it once on flip, so windows/repeat could never mature), via the sink only, no extra VM writes; (2) closes FIRING states whose series stopped reporting for `state-ttl` (container removed, disk gone) with `SERIES_GONE` and no notification, like Netdata's REMOVED; (3) evicts idle state; (4) re-emits REPEAT for events past `repeat_interval_seconds`. |
| `AlertDispatcher` | Bounded `ArrayBlockingQueue<AlertNotification>`; if full, drop and count. Virtual-thread worker: open/close `AlertEventRow`, load + decrypt the destination, coalesce events for the same destination within `coalesce-window` into one Discord message (up to 10 embeds), `destination.type().send(...)`. Retry with backoff; honour Discord 429 `Retry-After`; per-destination serialisation. Records `last_notified_at` / `notify_error`. |
| `AlertDestinationService` | Destination CRUD for the caller; validates + encrypts config; `test(id)` sends a sample message. |
| `AlertPreviewService` | Fetches history through `VictoriaMetricsQueryClient`, replays it through `AlertRuleEngine`, returns points + firing intervals. `AlertSeriesService` lists current label sets. |
| `AlertRuleService` | Rule CRUD for the caller; validates metric ∈ catalog, label keys, finite threshold, `window_seconds` range, `recovery_threshold` on the correct side of `threshold` for the operator, `monitor_server_up` forced to the offline-friendly shape, destination owned by caller; server-scoped needs `VIEW_METRICS` via `ServerService.requireServer`; per-user cap; invalidates cache; editing a rule closes its open events as `RULE_CHANGED`. `listEvents` paginated with existing `Pagination`. |
| `AlertStateRehydrator` | On `ApplicationReadyEvent`, seed FIRING state from open `alert_events` so restarts don't re-notify. Windows restart empty (rules cannot fire until covered again; acceptable). |

### Hook points

- `MetricWriteContext` (`metrics/vm/MetricWriteContext.java`): optional `MetricSink` (propagated by `withLabel`), invoked from `gauge(...)`; compute the merged label map once and share it between the writer and the sink.
- `ServerIngestService.ingestMetrics()`: construct the context with `alertEvaluator.sinkFor(server.getId(), now)`. `ingestBatch()` unchanged.
- `ServerService.markStaleServersOffline` / new sweeper: offline evaluation as above.
- `ServerAccessService` member removal / leave: delete that user's rules scoped to that server and invalidate; accept-invite invalidates the server.

## API (thin controllers, `@AuthenticatedUser`, typed exceptions from `exception.impl`)

- Destinations: `GET/POST /v1/alerts/destinations`, `PATCH/DELETE /v1/alerts/destinations/{id}`, `POST /v1/alerts/destinations/{id}/test`. Responses return `{id, name, type, maskedUrl, ruleCount}` only. Deleting a destination in use needs `?force=true` (it cascades its rules); the UI warns with the rule count.
- Rules: `GET/POST /v1/alerts/rules` (optional `serverId`; omitted = account-wide), `GET/PATCH/DELETE /v1/alerts/rules/{id}`, `POST /v1/alerts/rules/{id}/enabled`. Rule responses include `firingCount` and `lastFiredAt` (from open/last `alert_events`) so the list can show live state.
- Events (history): `GET /v1/alerts/events?page&count&ruleId&serverId&state=firing|resolved` (account-wide, caller's rules only) and the same filtered per server via `serverId`.
- Builder helpers:
  - `GET /v1/alerts/metrics`: alertable metrics with label, unit, family, label keys, curated flag, and a suggested default threshold (from `AlertMetricCatalog`).
  - `GET /v1/alerts/series?serverId&metric`: label sets currently reporting for that metric on that server (VM query via `VictoriaMetricsQueryClient`), to fill the label-filter dropdown (disk `/`, sensor `coretemp`...).
  - `POST /v1/alerts/preview`: body is a draft rule + `serverId` + range; returns the metric's history (VM `query_range`) and the firing intervals produced by running the **real `AlertEvaluator` state machine** (pure class, no I/O) over those points. One source of truth for window/hysteresis semantics, so the UI never re-implements them. Documented caveat: history is at VM step resolution, live evaluation uses raw samples.
- DTO records with Jakarta validation; permission/ownership checked in services as for the other endpoints.

## Config / observability

- `MonitorAlertProperties` (`monitor.alerts`): `queue-capacity`, `max-rules-per-user`, `coalesce-window`, `state-ttl`, `cache-ttl`, `http-timeout`, `max-window`.
- `PlatformMetricsRecorder`: counters `alert_evaluations`, `alert_transitions{type}`, `alert_notifications{status}`, `alert_dropped`; histogram `alert_sink_duration`; queue-depth gauge (existing accumulator classes).

## Frontend (`www/`): full alert builder

Stack and conventions per `.cursor/rules/typescript-react.mdc`, `project-conventions.mdc`, `monitor-design-system.mdc`: TanStack Start/Router file routes, React Query, zod 4, shadcn/Radix primitives, theme tokens (`text-brand`, `bg-primary`, `text-warning`, `text-error`), `function` components with named exports, no default memoisation, no hand edits to `routeTree.gen.ts`.

### Pages and routes (`src/routes/_authenticated/`)

| Route | Purpose |
|---|---|
| `alerts/route.tsx` | Layout: `PageHeader` + sub-tabs **Rules / Destinations / History**, same tab style as `ServerTabs` |
| `alerts/index.tsx` | Rules list (table via `ui/data-table.tsx` + TanStack Table, as `servers-table`): name, scope (account-wide / server), condition summary, severity badge, destination, live state (`OK` / `n firing`), enable toggle (`ui/settings-toggle.tsx`), row menu (edit, duplicate, delete with `confirm-dialog.tsx`). Empty state links to templates. |
| `alerts/new.tsx`, `alerts/$ruleId.tsx` | The builder (create / edit). Search params prefill it: `?template=cpu-high`, `?serverId=`, `?metric=` (so a later "alert on this chart" button is a one-liner). |
| `alerts/destinations.tsx` | Destination list + add/edit dialog (Discord webhook URL, masked after save, **Send test** button, "used by N rules" and delete warning). |
| `alerts/history.tsx` | Paginated events table (`pageSearchSchema`, `pagination.tsx`) with filters for rule, server, firing/resolved, showing started, duration, peak value, notify status/error. |
| `servers/$serverId/alerts.tsx` | New `ServerTabs` entry (bell icon): that server's effective rules, **including inherited account-wide ones** with an "overridden here" / "inherited" tag, a one-click per-server mute (creates a disabled override), recent events, and an "Add alert" button that opens the builder pre-scoped to the server. Gated by `VIEW_METRICS` via `hasPermission`. |
| Sidebar | Add **Alerts** (`Bell`) to `navItems` in `components/sidebar/sidebar-nav.tsx` (update the `to` union), with a count badge when any rule is firing. |

### Builder layout (single page, sections top to bottom, sticky summary on the right/bottom on mobile)

1. **Template picker** (create only): cards from `lib/alerts/presets.ts` (High CPU, High memory, Disk almost full, High temperature, Server offline, Start from scratch). Presets are frontend constants that prefill the draft; they carry no server logic.
2. **Scope**: "This server" (server `Select` from `useUserServers`) or "All my servers". Explains the override rule in one line.
3. **Metric**: searchable grouped picker (family groups, curated first) built on `popover.tsx` + the existing command-palette list pattern, fed by `GET /v1/alerts/metrics`. Selecting a metric sets unit, suggested threshold and the label-filter fields.
4. **Series filter** (only for labelled metrics): key/value rows limited to the family's label keys; for a server scope the value is a dropdown from `GET /v1/alerts/series` (disk `/`, sensor `coretemp`), for account-wide it is free text with "any" as default.
5. **Condition**: operator `Select` (above / below), threshold input with unit adornment (%, GB, °C, ...), **averaged over** `Select` (Instant, 1m, 5m, 10m, 30m, custom), severity (Warning / Critical). *Advanced* collapsible (`collapsible-panel.tsx`): recovery threshold (hysteresis) and repeat reminder (Off, 15m, 1h, 4h, 24h).
6. **Preview**: the real `MetricChart` (`components/metrics/metric-chart.tsx`, takes `uPlot.AlignedData` + `thresholds` + `valueFormatter`) showing the last 24h/7d for the chosen server/series, a dashed threshold line via the existing `createThresholdDrawHook` (colour by severity), and shaded periods where the rule would have fired, plus "Would have fired 3 times in the last 7 days" computed by `POST /v1/alerts/preview` (debounced, `keepPreviousData`). For account-wide scope the user picks which of their servers to preview against. No data / VM error states use `Callout`.
7. **Notify**: destination `Select` with inline **Add destination** (opens the same destination dialog) and **Send test**. Rule name (auto-filled, e.g. "High CPU usage"), enabled toggle.
8. **Summary sentence** (always visible): "Alert me when **CPU usage**, averaged over **5 minutes**, is **above 90%** on **all my servers**, then notify **Discord · #ops**. Reminds every **1 hour**." Built by `lib/alerts/summary.ts`, unit-tested.

Validation is a zod schema in `lib/alerts/schema.ts` mirroring the backend rules (finite threshold, window range, recovery threshold on the correct side of the threshold for the operator, `monitor_server_up` locks window/operator shape); field errors via `FormFieldError`; server errors via `toastMutationError`. Form state is local `useState` like `invite-member-dialog.tsx` (no new form library). Unsaved-changes guard on navigate.

### Files

- API layer: `src/lib/api/user/alerts.ts` (types + fetchers via `apiFetch`), `alerts.queries.ts` (query keys/options, rule list polls every 30s while anything is firing, like open incidents), mutation hooks in `src/hooks/` (`use-alert-rule-mutations.ts`, `use-alert-destinations.ts`, `use-alert-preview.ts`).
- Domain helpers: `src/lib/alerts/{schema,summary,presets,units}.ts`. `units.ts` maps the backend `AlertUnit` to the existing `lib/formatter.ts` functions (`formatPercentValue`, `formatMemoryBytes`, `formatCelsius`, ...), so there is no second formatter set.
- Components `src/components/alerts/`: `alert-builder.tsx` (container), `alert-template-picker`, `alert-scope-field`, `alert-metric-picker`, `alert-series-filter`, `alert-condition-fields`, `alert-advanced-fields`, `alert-destination-field`, `alert-preview-chart`, `alert-summary`, `alert-rules-table` (+ `alert-rule-columns`), `alert-events-table`, `destination-dialog`, `alert-severity-badge`, `alert-state-badge`.
- Reused: `PageHeader`, `QueryStatusShell`, `EmptyState`, `Callout`, `ConfirmDialog`, `Pagination`, `Spinner`, `SimpleTooltip`, `InfoHint`, `loadCachedQuery`, `authenticatedPageSectionClassName`.
- Tests (vitest, as `sparkline.test.ts`, `fleet-filter.test.ts`): `schema.test.ts`, `summary.test.ts`, `presets.test.ts` (every preset parses against the schema), plus a Testing Library test for the builder (template prefill, summary text, validation errors, submit payload, override notice).

## Implementation order

0. Copy plan to `plans/alerting.md`.
1. Backend: migration, entities, repositories.
2. Backend: `AlertOperator`, `AlertSeverity`, `AlertDestination`, `AlertMetricCatalog`/`AlertUnit`, `DiscordMessages` + unit tests (operator logic, unit formatting, markdown escaping, URL allowlist).
3. Backend: `MetricSink` in `MetricWriteContext` (+ tests: sink sees merged labels, no sink = identical output).
4. Backend: `AlertRuleEngine` (pure) + `AlertEvaluator` + `AlertStateSweeper` + unit tests with fake timestamps (window coverage, average vs spike, hysteresis, per-series state, label filter, override, vanished series, offline rule over several ticks, repeat).
5. Backend: `AlertRuleCache`, `AlertDispatcher`, `AlertDestinationTarget`; dispatcher test against a local `HttpServer` stub (200, 429 + Retry-After, 500, coalescing).
6. Backend: services, controllers (rules, destinations, events, metrics, series, preview), config properties, membership-removal hooks, wiring into `ServerIngestService`, rehydrator, platform metrics. Backend integration tests. **API contract is frozen here** so the frontend can build against it.
7. Frontend: API layer + queries + zod schema + units + summary + presets, with unit tests.
8. Frontend: destinations page and dialog (smallest end-to-end slice, proves the contract).
9. Frontend: rules list, then the builder (sections 2-5 and 7-8 first, preview chart last), edit/duplicate/delete, history page.
10. Frontend: server `Alerts` tab, sidebar entry with firing badge, empty/error/loading states, mobile layout pass.

## Verification

- Backend `./gradlew test`: unit tests above plus integration tests following `ServerIngestIntegrationTest` / `IntegrationTestBase`. Create a destination (non-Discord URL rejected, URL never echoed), server rule and account-wide rule, ingest samples across a window and assert one FIRING and one RESOLVED call per rule to a stub webhook. A per-server rule suppresses the account-wide one for that server, a disabled per-server rule mutes it, a removed member's rule stops firing, a backfill batch fires nothing, a labelled metric (`monitor_disk_usage_percent{disk="/"}`) only fires for the filtered disk, and `POST /v1/alerts/preview` yields the same firing intervals as live evaluation over the same points.
- Frontend (`cd www`): `bun run typecheck`, `bun run lint`, `bun run check`, `bun run test`.
- Manual end to end (`docker-compose` for API + Postgres + VM, `bun run dev` for www, a real agent, a real Discord webhook): add a destination and hit **Send test**; build "CPU above X% averaged over 1m" from the template with X just under current usage, confirm the preview shades the right period, the rule shows `1 firing`, the Discord message and later the resolve message arrive, and history shows the event; create an account-wide disk rule then a per-server override and confirm only one notification path; stop the agent and confirm the offline preset fires and resolves; try the builder at mobile width and in light/dark.
- Perf sanity: microbenchmark an ingest with 0 rules (sink is `null`, output identical) and with ~20 rules; confirm no DB query per ingest once the cache is warm.

## Follow-ups (not in this plan)

"Create alert from this chart" button on metric charts (builder already accepts `?metric=&serverId=`), two-threshold (warning+critical) rules, silences/maintenance windows, escalation, more `AlertDestination` constants (generic webhook, email via `MailService`), firing-alert indicators on server cards.

## Revision: account-wide alert groups (supersedes per-server scope and overrides)

Rules are no longer scoped to a server. Each rule belongs to an **alert group** owned by the user; a group has many
servers (`alert_group_servers`), and a server inherits the rules of every group it is in.

- Tables: `alert_groups(id, user_id, name, auto_add_servers, created_at)`, `alert_group_servers(group_id, server_id)`,
  `alert_rules.group_id` (replaces `server_id`). Migration `V10__alert_groups.sql` moves existing rules into a per-user
  "Default" group.
- New users get a "Default" group (auto-add on) seeded with the `AlertInfo.defaultRule` rules.
- Auto-add: when a user gains access to a server (owner or accepted invite) it joins their auto-add groups.
- Leaving or being removed from a server removes it from that user's groups and closes their open events for it.
- Deleting a group deletes its rules; removing a server from a group closes that server's open events for the group's rules.
- No override/mute logic any more: to exclude a server, leave it out of the group.
- API: `GET/POST /v1/alerts/groups`, `GET/PATCH/DELETE /v1/alerts/groups/{id}`; rules take `groupId` (immutable after create)
  and `GET /v1/alerts/rules?groupId=`.
- Frontend: Groups tab (list, create/edit dialog with server multi-select), group page with its rules, rule builder under
  `/alerts/groups/$groupId/rules/{new,$ruleId}`, History tab.
