#!/usr/bin/env python3
"""Migrate servers-dashboard.json metric panels from Postgres to VictoriaMetrics/PromQL."""

from __future__ import annotations

import json
import re
from copy import deepcopy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DASHBOARD = ROOT / "servers-dashboard.json"
PG_DS = "ffnexhw0yascgf"
VM_DS = "$datasource"
SERVER = 'server_id="${server}"'

# SQL fragments that stay on Postgres (inventory / server metadata).
PG_KEEP_PATTERNS = (
    "FROM servers WHERE",
    "FROM server_inventory WHERE",
    "FROM servers ORDER BY",
)


def prom_data_query(expr: str, ref_id: str, *, instant: bool = False, range_q: bool = True, legend: str = "__auto") -> dict:
    return {
        "datasource": {"name": VM_DS},
        "group": "prometheus",
        "kind": "DataQuery",
        "spec": {
            "expr": expr,
            "refId": ref_id,
            "instant": instant,
            "range": range_q,
            "legendFormat": legend,
        },
        "version": "v0",
    }


def panel_query_from_prom(expr: str, ref_id: str, **kwargs) -> dict:
    q = prom_data_query(expr, ref_id, **kwargs)
    return {
        "kind": "PanelQuery",
        "spec": {
            "hidden": False,
            "query": q,
            "refId": ref_id,
        },
    }


def normalize_sql(sql: str) -> str:
    return re.sub(r"\s+", " ", sql.replace("\r\n", " ").replace("\r", " ")).strip()


def keep_postgres(sql: str) -> bool:
    n = normalize_sql(sql)
    return any(p in n for p in PG_KEEP_PATTERNS)


def disk_matcher() -> str:
    return 'disk=~"$drive"'


def iface_matcher() -> str:
    return 'interface=~"$interface"'


def pool_matcher() -> str:
    return 'pool=~"$pool"'


def host_series(metric: str, legend: str, ref: str) -> dict:
    return panel_query_from_prom(f"avg({metric}{{{SERVER}}})", ref, legend=legend)


def disk_series(metric: str, legend: str, ref: str, *, per_drive: bool = False) -> dict:
    labels = SERVER
    if per_drive:
        labels += f", {disk_matcher()}"
    return panel_query_from_prom(f"avg({metric}{{{labels}}})", ref, legend=legend)


def net_series(metric: str, legend: str, ref: str, *, per_iface: bool = False, scale: float = 1) -> dict:
    labels = SERVER
    if per_iface:
        labels += f", {iface_matcher()}"
    expr = f"sum({metric}{{{labels}}})"
    if scale != 1:
        expr = f"({expr}) * {scale}"
    return panel_query_from_prom(expr, ref, legend=legend)


def pool_series(metric: str, legend: str, ref: str, *, per_pool: bool = False, agg: str = "avg") -> dict:
    labels = SERVER
    if per_pool:
        labels += f", {pool_matcher()}"
    return panel_query_from_prom(f"{agg}({metric}{{{labels}}})", ref, legend=legend)


def zfs_arc_series(metric: str, legend: str, ref: str) -> dict:
    return panel_query_from_prom(f"avg({metric}{{{SERVER}}})", ref, legend=legend)


def docker_series(metric: str, ref: str) -> dict:
    return panel_query_from_prom(
        f"avg by (container) ({metric}{{{SERVER}}})",
        ref,
        legend="{{container}}",
    )


def translate_sql(raw_sql: str, ref_id: str) -> list[dict] | None:
    """Return PromQL panel queries, or None to keep Postgres."""
    if keep_postgres(raw_sql):
        return None

    sql = normalize_sql(raw_sql)

    # --- Variables (label_values) ---
    if "disk_name AS __text" in sql and "server_disk_metrics" in sql:
        return None  # handled in migrate_variable

    if "interface_name AS __text" in sql and "server_network_metrics" in sql:
        return None

    if "pool_name AS __text" in sql and "server_zfs_pool_metrics" in sql:
        return None

    disk_labels = f"{{{SERVER}, {disk_matcher()}}}"
    pool_labels = f"{{{SERVER}, {pool_matcher()}}}"

    instant = translate_instant(sql, ref_id, disk_labels, pool_labels)
    if instant:
        return instant

    # Docker multi-series
    if "server_docker_container_metrics" in sql and "container_name AS metric" in sql:
        if "cpu_usage" in sql:
            return [docker_series("monitor_container_cpu_usage", ref_id)]
        if "memory_usage" in sql:
            return [docker_series("monitor_container_memory_usage", ref_id)]

    # Daily disk change (approximation)
    if sql.startswith("WITH day_starts AS"):
        expr = (
            f"monitor_disk_used_bytes{disk_labels} "
            f"- min_over_time(monitor_disk_used_bytes{disk_labels}[1d])"
        )
        return [panel_query_from_prom(expr, ref_id, legend="Daily Change")]

    if sql.startswith("WITH visible_buckets AS"):
        expr = (
            f"(monitor_disk_total_bytes{disk_labels} - monitor_disk_used_bytes{disk_labels}) "
            f"/ clamp_min("
            f"(monitor_disk_used_bytes{disk_labels} "
            f"- monitor_disk_used_bytes{disk_labels} offset 1d) / 86400, 1)"
        )
        return [panel_query_from_prom(expr, ref_id, legend="ETA until full")]

    # Multi-metric time series from one SQL
    multi = parse_multi_series_sql(sql)
    if multi:
        out = []
        for i, (expr, legend) in enumerate(multi):
            rid = ref_id if i == 0 else chr(ord("A") + i)
            out.append(panel_query_from_prom(expr, rid, legend=legend))
        return out

    # Single-series time series
    single = translate_single_timeseries(sql)
    if single:
        expr, legend = single
        return [panel_query_from_prom(expr, ref_id, legend=legend)]

    raise ValueError(f"Unmapped SQL: {sql[:200]}...")


def translate_instant(
    sql: str, ref_id: str, disk_labels: str, pool_labels: str
) -> list[dict] | None:
    instant_map: dict[str, tuple[str, str]] = {
        normalize_sql(
            "SELECT cpu_usage FROM server_metrics WHERE server_id = '${server}' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_host_cpu_usage{{{SERVER}}}[15m])", "__auto"),
        normalize_sql(
            "SELECT mem_available AS memory_used FROM server_metrics WHERE server_id = '${server}' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_host_mem_available{{{SERVER}}}[15m])", "__auto"),
        normalize_sql(
            "SELECT mem_usage AS memory_used FROM server_metrics WHERE server_id = '${server}' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_host_mem_usage{{{SERVER}}}[15m])", "__auto"),
        normalize_sql(
            "SELECT cpu_clock_mhz * 1000000 AS cpu_clock_hz FROM server_inventory WHERE server_id = '${server}'"
        ): (f"last_over_time(monitor_host_cpu_clock_mhz{{{SERVER}}}[15m]) * 1e6", "__auto"),
        normalize_sql(
            "SELECT usage_pct FROM server_disk_metrics WHERE server_id = '${server}' AND disk_name = '$drive' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_disk_usage_percent{disk_labels}[15m])", "__auto"),
        normalize_sql(
            "SELECT used_bytes FROM server_disk_metrics WHERE server_id = '${server}' AND disk_name = '$drive' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_disk_used_bytes{disk_labels}[15m])", "__auto"),
        normalize_sql(
            "SELECT total_bytes - used_bytes AS free_bytes FROM server_disk_metrics WHERE server_id = '${server}' AND disk_name = '$drive' ORDER BY timestamp DESC LIMIT 1"
        ): (
            f"last_over_time(monitor_disk_total_bytes{disk_labels}[15m]) "
            f"- last_over_time(monitor_disk_used_bytes{disk_labels}[15m])",
            "__auto",
        ),
        normalize_sql(
            "SELECT io_wait_ms FROM server_disk_metrics WHERE server_id = '${server}' AND disk_name = '$drive' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_disk_io_wait_ms{disk_labels}[15m])", "__auto"),
        normalize_sql(
            "SELECT (COALESCE(read_latency_ms, 0) + COALESCE(write_latency_ms, 0)) / 2.0 AS io_delay FROM server_disk_metrics WHERE server_id = '${server}' AND disk_name = '$drive' ORDER BY timestamp DESC LIMIT 1"
        ): (
            f"(last_over_time(monitor_disk_read_latency_ms{disk_labels}[15m]) "
            f"+ last_over_time(monitor_disk_write_latency_ms{disk_labels}[15m])) / 2",
            "__auto",
        ),
        normalize_sql(
            "SELECT health FROM server_zfs_pool_metrics WHERE server_id = '${server}' AND pool_name = '$pool' ORDER BY timestamp DESC LIMIT 1"
        ): (f"last_over_time(monitor_zfs_pool_total_bytes{pool_labels}[15m])", "{{health}}"),
    }
    if sql in instant_map:
        expr, legend = instant_map[sql]
        return [
            panel_query_from_prom(expr, ref_id, instant=True, range_q=False, legend=legend)
        ]

    if sql.startswith(
        "SELECT latest.used_bytes - day_start.used_bytes AS change_today FROM"
    ):
        expr = (
            f"last_over_time(monitor_disk_used_bytes{disk_labels}[15m]) "
            f"- min_over_time(monitor_disk_used_bytes{disk_labels}[1d])"
        )
        return [
            panel_query_from_prom(expr, ref_id, instant=True, range_q=False, legend="__auto")
        ]
    return None


def parse_multi_series_sql(sql: str) -> list[tuple[str, str]] | None:
    """Parse avg(x) AS \"Label\" columns from grouped time-series SQL."""
    if "$__timeGroupAlias" not in sql and "SELECT time," not in sql:
        return None

    # Subquery aggregates (pool/disk totals) handled separately
    if "FROM (SELECT" in sql and "per_disk" in sql:
        return translate_subquery_aggregate(sql)
    if "FROM (SELECT" in sql and "per_pool" in sql:
        return translate_subquery_aggregate(sql)

    matches = re.findall(
        r"(?:avg|AVG|sum|SUM)\(([^)]+)\)(?:\s*\*\s*8)?\s+AS\s+\"([^\"]+)\"",
        sql,
    )
    if len(matches) < 2:
        return None

    per_drive = "disk_name = '$drive'" in sql
    per_pool = "pool_name = '$pool'" in sql
    per_iface = "interface_name = '$interface'" in sql

    out: list[tuple[str, str]] = []
    for col, legend in matches:
        expr = column_to_prom_expr(col, sql, per_drive=per_drive, per_pool=per_pool, per_iface=per_iface)
        out.append((expr, legend))
    return out


def translate_subquery_aggregate(sql: str) -> list[tuple[str, str]] | None:
    """Disk/pool rolled-up subqueries."""
    if "per_disk" in sql and "SUM(used_bytes)" in sql and "SUM(total_bytes)" in sql:
        return None
    if "per_disk" in sql and "SUM(used_bytes) AS \"Used\"" in sql and "inode" not in sql.lower():
        if "SUM(total_bytes)" in sql:
            return [
                (
                    f"sum(monitor_disk_used_bytes{{{SERVER}}})",
                    "Used",
                ),
                (
                    f"sum(monitor_disk_total_bytes{{{SERVER}}})",
                    "Total",
                ),
            ]
        return [(f"sum(monitor_disk_used_bytes{{{SERVER}}})", "Used")]
    if "per_disk" in sql and "SUM(total_bytes) AS \"Total\"" in sql:
        return [(f"sum(monitor_disk_total_bytes{{{SERVER}}})", "Total")]
    if "per_disk" in sql and "usage" in sql.lower():
        return [
            (
                f"sum(monitor_disk_used_bytes{{{SERVER}}}) / sum(monitor_disk_total_bytes{{{SERVER}}}) * 100",
                "Usage",
            )
        ]
    if "per_disk" in sql and "inode_used" in sql:
        return [
            (f"sum(monitor_disk_inode_used{{{SERVER}}})", "Used"),
            (f"sum(monitor_disk_inode_total{{{SERVER}}})", "Total"),
        ]
    if "per_pool" in sql and "SUM(allocated_bytes)" in sql:
        return [
            (f"sum(monitor_zfs_pool_allocated_bytes{{{SERVER}}})", "Allocated"),
            (f"sum(monitor_zfs_pool_free_bytes{{{SERVER}}})", "Free"),
            (f"sum(monitor_zfs_pool_total_bytes{{{SERVER}}})", "Total"),
        ]
    if "per_pool" in sql and "capacity_percent" in sql and "SUM(" not in sql.upper():
        return [(f"avg(monitor_zfs_pool_capacity_percent{{{SERVER}}})", "Capacity")]
    if "per_pool" in sql and "SUM(read_bps)" in sql:
        return [
            (f"sum(monitor_zfs_pool_read_bps{{{SERVER}}})", "Read"),
            (f"sum(monitor_zfs_pool_write_bps{{{SERVER}}})", "Write"),
        ]
    if "per_pool" in sql and "SUM(read_iops)" in sql:
        return [
            (f"sum(monitor_zfs_pool_read_iops{{{SERVER}}})", "Read"),
            (f"sum(monitor_zfs_pool_write_iops{{{SERVER}}})", "Write"),
        ]
    if "per_pool" in sql and "fragmentation_percent" in sql:
        return [(f"avg(monitor_zfs_pool_fragmentation_percent{{{SERVER}}})", "Fragmentation")]
    return None


COLUMN_METRIC = {
    "load_1": ("monitor_host_load_1", "host"),
    "load_5": ("monitor_host_load_5", "host"),
    "load_15": ("monitor_host_load_15", "host"),
    "cpu_usage": ("monitor_host_cpu_usage", "host"),
    "cpu_user_pct": ("monitor_host_cpu_user_pct", "host"),
    "cpu_system_pct": ("monitor_host_cpu_system_pct", "host"),
    "cpu_iowait_pct": ("monitor_host_cpu_iowait_pct", "host"),
    "cpu_steal_pct": ("monitor_host_cpu_steal_pct", "host"),
    "mem_usage": ("monitor_host_mem_usage", "host"),
    "mem_available": ("monitor_host_mem_available", "host"),
    "mem_total": ("monitor_host_mem_total", "host"),
    "mem_buffers": ("monitor_host_mem_buffers", "host"),
    "mem_cached": ("monitor_host_mem_cached", "host"),
    "swap_used": ("monitor_host_swap_used", "host"),
    "swap_total": ("monitor_host_swap_total", "host"),
    "process_count": ("monitor_host_process_count", "host"),
    "running_processes": ("monitor_host_running_processes", "host"),
    "ctx_switches_per_second": ("monitor_host_ctx_switches_per_second", "host"),
    "interrupts_per_second": ("monitor_host_interrupts_per_second", "host"),
    "usage_pct": ("monitor_disk_usage_percent", "disk"),
    "used_bytes": ("monitor_disk_used_bytes", "disk"),
    "total_bytes": ("monitor_disk_total_bytes", "disk"),
    "io_usage_pct": ("monitor_disk_io_usage_pct", "disk"),
    "io_read_bps": ("monitor_disk_io_read_bps", "disk"),
    "io_write_bps": ("monitor_disk_io_write_bps", "disk"),
    "io_wait_ms": ("monitor_disk_io_wait_ms", "disk"),
    "inode_used": ("monitor_disk_inode_used", "disk"),
    "inode_total": ("monitor_disk_inode_total", "disk"),
    "read_iops": ("monitor_disk_read_iops", "disk"),
    "write_iops": ("monitor_disk_write_iops", "disk"),
    "read_latency_ms": ("monitor_disk_read_latency_ms", "disk"),
    "write_latency_ms": ("monitor_disk_write_latency_ms", "disk"),
    "rx_errors_per_second": ("monitor_net_rx_errors_per_second", "net"),
    "tx_errors_per_second": ("monitor_net_tx_errors_per_second", "net"),
    "rx_bps": ("monitor_net_rx_bps", "net"),
    "tx_bps": ("monitor_net_tx_bps", "net"),
    "rx_packets_per_second": ("monitor_net_rx_packets_per_second", "net"),
    "tx_packets_per_second": ("monitor_net_tx_packets_per_second", "net"),
    "arc_misses_per_second": ("monitor_zfs_arc_misses_per_second", "arc"),
    "arc_size_bytes": ("monitor_zfs_arc_size_bytes", "arc"),
    "arc_target_bytes": ("monitor_zfs_arc_target_bytes", "arc"),
    "arc_max_bytes": ("monitor_zfs_arc_max_bytes", "arc"),
    "arc_min_bytes": ("monitor_zfs_arc_min_bytes", "arc"),
    "arc_data_bytes": ("monitor_zfs_arc_data_bytes", "arc"),
    "arc_metadata_bytes": ("monitor_zfs_arc_metadata_bytes", "arc"),
    "l2arc_size_bytes": ("monitor_zfs_arc_l2arc_size_bytes", "arc"),
    "arc_hit_ratio": ("monitor_zfs_arc_hit_ratio", "arc"),
    "capacity_percent": ("monitor_zfs_pool_capacity_percent", "pool"),
    "allocated_bytes": ("monitor_zfs_pool_allocated_bytes", "pool"),
    "free_bytes": ("monitor_zfs_pool_free_bytes", "pool"),
    "total_bytes_pool": ("monitor_zfs_pool_total_bytes", "pool"),
    "fragmentation_percent": ("monitor_zfs_pool_fragmentation_percent", "pool"),
    "scan_percent": ("monitor_zfs_pool_scan_percent", "pool"),
    "read_bps": ("monitor_zfs_pool_read_bps", "pool"),
    "write_bps": ("monitor_zfs_pool_write_bps", "pool"),
    "read_iops_pool": ("monitor_zfs_pool_read_iops", "pool"),
    "write_iops_pool": ("monitor_zfs_pool_write_iops", "pool"),
    "checksum_errors": ("monitor_zfs_pool_checksum_errors", "pool"),
}


def column_to_prom_expr(col: str, sql: str, *, per_drive: bool, per_pool: bool, per_iface: bool) -> str:
    col = col.strip()
    kind = "host"
    if "server_zfs_pool_metrics" in sql:
        kind = "pool"
        pool_cols = {
            "total_bytes": "monitor_zfs_pool_total_bytes",
            "read_iops": "monitor_zfs_pool_read_iops",
            "write_iops": "monitor_zfs_pool_write_iops",
            "read_bps": "monitor_zfs_pool_read_bps",
            "write_bps": "monitor_zfs_pool_write_bps",
            "capacity_percent": "monitor_zfs_pool_capacity_percent",
            "allocated_bytes": "monitor_zfs_pool_allocated_bytes",
            "free_bytes": "monitor_zfs_pool_free_bytes",
            "fragmentation_percent": "monitor_zfs_pool_fragmentation_percent",
            "scan_percent": "monitor_zfs_pool_scan_percent",
            "checksum_errors": "monitor_zfs_pool_checksum_errors",
        }
        metric = pool_cols.get(col)
        if metric is None and col in COLUMN_METRIC:
            metric, kind = COLUMN_METRIC[col]
        elif metric is None:
            raise ValueError(f"unknown pool column {col}")
    elif "server_disk_metrics" in sql:
        kind = "disk"
        entry = COLUMN_METRIC.get(col)
        if not entry:
            raise ValueError(f"unknown disk column {col}")
        metric, _ = entry
    elif "server_network_metrics" in sql:
        kind = "net"
        entry = COLUMN_METRIC.get(col)
        if not entry:
            raise ValueError(f"unknown net column {col}")
        metric, _ = entry
    elif "server_zfs_arc_metrics" in sql:
        kind = "arc"
        entry = COLUMN_METRIC.get(col)
        if not entry:
            raise ValueError(f"unknown arc column {col}")
        metric, _ = entry
    else:
        entry = COLUMN_METRIC.get(col)
        if not entry:
            raise ValueError(f"unknown column {col}")
        metric, kind = entry

    labels = SERVER
    use_sum = "SUM(" in sql.upper() and kind in ("disk", "net", "pool")
    fn = "sum" if use_sum else "avg"

    if per_drive or (kind == "disk" and "disk_name = '$drive'" in sql):
        labels += f", {disk_matcher()}"
    if per_pool or (kind == "pool" and "pool_name = '$pool'" in sql):
        labels += f", {pool_matcher()}"
    if per_iface or (kind == "net" and "interface_name = '$interface'" in sql):
        labels += f", {iface_matcher()}"

    expr = f"{fn}({metric}{{{labels}}})"
    if kind == "net" and ("rx_bps" in col or "tx_bps" in col) and "* 8" in sql:
        expr = f"({expr}) * 8"
    return expr


def translate_single_timeseries(sql: str) -> tuple[str, str] | None:
    multi = parse_multi_series_sql(sql)
    if multi and len(multi) == 1:
        return multi[0]

    m = re.search(
        r"(?:avg|AVG|sum|SUM)\(([^)]+)\)(?:\s*\*\s*8)?\s+AS\s+\"([^\"]+)\"",
        sql,
    )
    if m and "$__timeGroupAlias" in sql:
        expr = column_to_prom_expr(
            m.group(1),
            sql,
            per_drive="disk_name = '$drive'" in sql,
            per_pool="pool_name = '$pool'" in sql,
            per_iface="interface_name = '$interface'" in sql,
        )
        return expr, m.group(2)

    if "SELECT time," in sql and "per_disk" in sql:
        sub = translate_subquery_aggregate(sql)
        if sub and len(sub) == 1:
            return sub[0]
    if "SELECT time," in sql and "per_pool" in sql:
        sub = translate_subquery_aggregate(sql)
        if sub and len(sub) == 1:
            return sub[0]

    if "SELECT time," in sql and "per_disk" in sql:
        sub = translate_subquery_aggregate(sql)
        if sub and len(sub) == 1:
            return sub[0]

    # Weighted disk usage subquery
    if "NULLIF(SUM(total_bytes)" in sql:
        return (
            f"sum(monitor_disk_used_bytes{{{SERVER}}}) / sum(monitor_disk_total_bytes{{{SERVER}}}) * 100",
            "Usage",
        )
    return None


def migrate_variable(var: dict) -> None:
    spec = var.get("spec", {})
    name = spec.get("name")
    q = spec.get("query", {})
    if q.get("group") != "grafana-postgresql-datasource":
        return

    sql = normalize_sql(spec.get("definition", "") or q.get("spec", {}).get("rawSql", ""))
    prom_query = None
    if name == "drive":
        prom_query = f"label_values(monitor_disk_used_bytes{{{SERVER}}}, disk)"
    elif name == "interface":
        prom_query = f"label_values(monitor_net_rx_bps{{{SERVER}}}, interface)"
    elif name == "pool":
        prom_query = f"label_values(monitor_zfs_pool_capacity_percent{{{SERVER}}}, pool)"
    else:
        return

    spec["definition"] = prom_query
    spec["query"] = {
        "datasource": {"name": VM_DS},
        "group": "prometheus",
        "kind": "DataQuery",
        "spec": {
            "query": prom_query,
            "refId": "VariableQuery",
        },
        "version": "v0",
    }


def migrate_query_group(spec: dict) -> None:
    queries = spec.get("queries", [])
    new_queries: list[dict] = []
    for pq in queries:
        query = pq.get("spec", {}).get("query", {})
        if query.get("group") != "grafana-postgresql-datasource":
            new_queries.append(pq)
            continue
        raw_sql = query.get("spec", {}).get("rawSql") or query.get("spec", {}).get("query", "")
        ref_id = pq.get("spec", {}).get("refId", "A")
        translated = translate_sql(raw_sql, ref_id)
        if translated is None:
            new_queries.append(pq)
        else:
            new_queries.extend(translated)
    spec["queries"] = new_queries


def walk(obj: object) -> None:
    if isinstance(obj, dict):
        if obj.get("kind") == "QueryGroup" and "spec" in obj:
            migrate_query_group(obj["spec"])
        for v in obj.values():
            walk(v)
    elif isinstance(obj, list):
        for item in obj:
            walk(item)


def main() -> None:
    data = json.loads(DASHBOARD.read_text(encoding="utf-8"))
    for var in data.get("variables", []):
        migrate_variable(var)
    walk(data)
    DASHBOARD.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {DASHBOARD}")


if __name__ == "__main__":
    main()
