#!/usr/bin/env python3
"""Generate api-dashboard.json in Grafana format matching servers-dashboard.json."""

import json
from pathlib import Path

PG_DS = "ffnexhw0yascgf"
PROM_DS_VAR = "prometheus"
DATASET = "monitor"
VIZ_VER = "13.0.1+security-01"
ROOT = Path(__file__).resolve().parents[1]


def pg_query(sql: str, ref: str = "A", fmt: str = "table") -> dict:
    return {
        "datasource": {"name": PG_DS},
        "group": "grafana-postgresql-datasource",
        "kind": "DataQuery",
        "spec": {
            "dataset": DATASET,
            "editorMode": "code",
            "format": fmt,
            "rawQuery": True,
            "rawSql": sql,
            "table": "servers",
        },
        "version": "v0",
    }


def prom_query(expr: str, ref: str = "A", instant: bool = False, range_q: bool = True) -> dict:
    return {
        "datasource": {"name": PROM_DS},
        "group": "prometheus",
        "kind": "DataQuery",
        "spec": {
            "expr": expr,
            "refId": ref,
            "instant": instant,
            "range": range_q,
            "legendFormat": "__auto",
        },
        "version": "v0",
    }


def panel_query(q: dict) -> dict:
    return {
        "kind": "PanelQuery",
        "spec": {
            "hidden": False,
            "query": q,
            "refId": q["spec"].get("refId", "A"),
        },
    }


def data_group(queries: list[dict]) -> dict:
    return {
        "kind": "QueryGroup",
        "spec": {
            "queries": [panel_query(q) for q in queries],
            "queryOptions": {},
            "transformations": [],
        },
    }


def stat_viz(unit: str = "none") -> dict:
    return {
        "group": "stat",
        "kind": "VizConfig",
        "spec": {
            "fieldConfig": {
                "defaults": {
                    "color": {"mode": "thresholds"},
                    "thresholds": {
                        "mode": "absolute",
                        "steps": [{"color": "green", "value": None}],
                    },
                    "unit": unit,
                },
                "overrides": [],
            },
            "options": {
                "colorMode": "value",
                "graphMode": "none",
                "justifyMode": "auto",
                "orientation": "auto",
                "percentChangeColorMode": "standard",
                "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
                "showPercentChange": False,
                "textMode": "auto",
                "wideLayout": True,
            },
        },
        "version": VIZ_VER,
    }


def timeseries_viz(unit: str = "short") -> dict:
    return {
        "group": "timeseries",
        "kind": "VizConfig",
        "spec": {
            "fieldConfig": {
                "defaults": {
                    "color": {"mode": "palette-classic"},
                    "custom": {
                        "axisBorderShow": False,
                        "axisCenteredZero": False,
                        "axisColorMode": "text",
                        "axisLabel": "",
                        "axisPlacement": "auto",
                        "barAlignment": 0,
                        "barWidthFactor": 0.6,
                        "drawStyle": "line",
                        "fillOpacity": 10,
                        "gradientMode": "none",
                        "hideFrom": {"legend": False, "tooltip": False, "viz": False},
                        "insertNulls": False,
                        "lineInterpolation": "linear",
                        "lineWidth": 1,
                        "pointSize": 5,
                        "scaleDistribution": {"type": "linear"},
                        "showPoints": "never",
                        "showValues": False,
                        "spanNulls": False,
                        "stacking": {"group": "A", "mode": "none"},
                        "thresholdsStyle": {"mode": "off"},
                    },
                    "min": 0,
                    "unit": unit,
                },
                "overrides": [],
            },
            "options": {
                "legend": {
                    "calcs": [],
                    "displayMode": "list",
                    "placement": "bottom",
                    "showLegend": True,
                },
                "tooltip": {"hideZeros": False, "mode": "single", "sort": "none"},
            },
        },
        "version": VIZ_VER,
    }


def pie_viz() -> dict:
    return {
        "group": "piechart",
        "kind": "VizConfig",
        "spec": {
            "fieldConfig": {
                "defaults": {"color": {"mode": "palette-classic"}, "unit": "short"},
                "overrides": [],
            },
            "options": {
                "legend": {
                    "displayMode": "table",
                    "placement": "right",
                    "showLegend": True,
                    "values": ["value"],
                },
                "pieType": "pie",
                "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
                "tooltip": {"mode": "single", "sort": "none"},
            },
        },
        "version": VIZ_VER,
    }


def barchart_viz(unit: str = "short") -> dict:
    return {
        "group": "barchart",
        "kind": "VizConfig",
        "spec": {
            "fieldConfig": {
                "defaults": {"color": {"mode": "palette-classic"}, "unit": unit},
                "overrides": [],
            },
            "options": {
                "legend": {"displayMode": "list", "placement": "bottom", "showLegend": False},
                "orientation": "horizontal",
                "showValue": "auto",
                "stacking": "none",
                "xTickLabelRotation": 0,
            },
        },
        "version": VIZ_VER,
    }


def make_panel(pid: int, title: str, queries: list[dict], viz: dict, desc: str = "") -> dict:
    group = data_group(queries)
    return {
        "kind": "Panel",
        "spec": {
            "data": {"kind": group["kind"], "spec": group["spec"]},
            "description": desc,
            "id": pid,
            "links": [],
            "title": title,
            "vizConfig": viz,
        },
    }


def grid_item(name: str, x: int, y: int, w: int, h: int) -> dict:
    return {
        "kind": "GridLayoutItem",
        "spec": {
            "element": {"kind": "ElementReference", "name": name},
            "height": h,
            "width": w,
            "x": x,
            "y": y,
        },
    }


def row(title: str, items: list[dict]) -> dict:
    return {
        "kind": "RowsLayoutRow",
        "spec": {
            "collapse": False,
            "layout": {"kind": "GridLayout", "spec": {"items": items}},
            "title": title,
        },
    }


def main() -> None:
    elements: dict = {}

    def add(pid: int, title: str, queries: list[dict], viz: dict, desc: str = "") -> None:
        elements[f"panel-{pid}"] = make_panel(pid, title, queries, viz, desc)

    five_min = "5 minutes"
    twenty_four_h = "24 hours"

    add(1, "Users", [pg_query("SELECT COUNT(*)::bigint AS value FROM users")], stat_viz())
    add(2, "Servers", [pg_query("SELECT COUNT(*)::bigint AS value FROM servers")], stat_viz())
    add(
        3,
        "Reporting",
        [
            pg_query(
                f"SELECT COUNT(*)::bigint AS value FROM servers "
                f"WHERE last_updated > NOW() - INTERVAL '{five_min}'"
            )
        ],
        stat_viz(),
    )
    add(4, "Offline", [pg_query("SELECT COUNT(*)::bigint AS value FROM servers WHERE status = 1")], stat_viz())
    add(
        5,
        "Stale",
        [
            pg_query(
                f"SELECT COUNT(*)::bigint AS value FROM servers "
                f"WHERE last_updated IS NOT NULL AND last_updated <= NOW() - INTERVAL '{five_min}'"
            )
        ],
        stat_viz(),
    )
    add(
        6,
        "Never Reported",
        [pg_query("SELECT COUNT(*)::bigint AS value FROM servers WHERE last_updated IS NULL")],
        stat_viz(),
    )
    add(
        7,
        "Ingests / min",
        [prom_query("rate(monitor_ingests_total[1m]) * 60", instant=True, range_q=False)],
        stat_viz("rpm"),
        "Requires Prometheus scraping GET /metrics",
    )
    add(
        8,
        "Auth Failures / min",
        [prom_query("rate(monitor_ingest_auth_failures_total[1m]) * 60", instant=True, range_q=False)],
        stat_viz("rpm"),
    )
    add(
        9,
        "Database Size",
        [pg_query("SELECT pg_database_size(current_database())::bigint AS value")],
        stat_viz("bytes"),
    )

    add(
        10,
        "New Users (24h)",
        [
            pg_query(
                f"SELECT COUNT(*)::bigint AS value FROM users "
                f"WHERE created_at > NOW() - INTERVAL '{twenty_four_h}'"
            )
        ],
        stat_viz(),
    )
    add(
        11,
        "New Servers (24h)",
        [
            pg_query(
                f"SELECT COUNT(*)::bigint AS value FROM servers "
                f"WHERE created_at > NOW() - INTERVAL '{twenty_four_h}'"
            )
        ],
        stat_viz(),
    )
    add(14, "Ingest Rate", [prom_query("rate(monitor_ingests_total[1m]) * 60")], timeseries_viz("rpm"))
    add(
        16,
        "Ingest Duration",
        [
            prom_query(
                "histogram_quantile(0.50, sum(rate(monitor_ingest_duration_seconds_bucket[5m])) by (le))",
                "A",
            ),
            prom_query(
                "histogram_quantile(0.95, sum(rate(monitor_ingest_duration_seconds_bucket[5m])) by (le))",
                "B",
            ),
            prom_query(
                "histogram_quantile(0.99, sum(rate(monitor_ingest_duration_seconds_bucket[5m])) by (le))",
                "C",
            ),
        ],
        timeseries_viz("s"),
    )
    add(
        17,
        "Auth Failures",
        [prom_query("rate(monitor_ingest_auth_failures_total[1m]) * 60")],
        timeseries_viz("rpm"),
    )

    add(
        18,
        "Agent Versions",
        [
            pg_query(
                "SELECT COALESCE(NULLIF(agent_version, ''), 'unknown') AS metric, "
                "COUNT(*)::bigint AS value FROM servers "
                "GROUP BY agent_version ORDER BY value DESC"
            )
        ],
        pie_viz(),
    )
    add(
        19,
        "Operating Systems",
        [
            pg_query(
                "SELECT COALESCE(NULLIF(si.os_name, ''), 'unknown') AS metric, "
                "COUNT(*)::bigint AS value FROM servers s "
                "LEFT JOIN server_inventory si ON si.server_id = s.id "
                "GROUP BY si.os_name ORDER BY value DESC"
            )
        ],
        pie_viz(),
    )

    layout_rows = [
        row(
            "Overview",
            [
                grid_item("panel-1", 0, 0, 4, 4),
                grid_item("panel-2", 4, 0, 4, 4),
                grid_item("panel-3", 8, 0, 4, 4),
                grid_item("panel-4", 12, 0, 4, 4),
                grid_item("panel-5", 16, 0, 4, 4),
                grid_item("panel-6", 20, 0, 4, 4),
                grid_item("panel-7", 0, 4, 8, 4),
                grid_item("panel-8", 8, 4, 8, 4),
                grid_item("panel-9", 16, 4, 8, 4),
            ],
        ),
        row(
            "Growth",
            [
                grid_item("panel-10", 0, 0, 12, 4),
                grid_item("panel-11", 12, 0, 12, 4),
            ],
        ),
        row(
            "Ingest",
            [
                grid_item("panel-14", 0, 0, 12, 8),
                grid_item("panel-16", 12, 0, 12, 8),
                grid_item("panel-17", 0, 8, 24, 8),
            ],
        ),
        row(
            "Fleet",
            [
                grid_item("panel-18", 0, 0, 12, 10),
                grid_item("panel-19", 12, 0, 12, 10),
            ],
        ),
    ]

    dashboard = {
        "annotations": [
            {
                "kind": "AnnotationQuery",
                "spec": {
                    "builtIn": True,
                    "enable": True,
                    "hide": True,
                    "iconColor": "rgba(0, 211, 255, 1)",
                    "name": "Annotations & Alerts",
                    "query": {
                        "datasource": {"name": "-- Grafana --"},
                        "group": "grafana",
                        "kind": "DataQuery",
                        "spec": {},
                        "version": "v0",
                    },
                },
            }
        ],
        "cursorSync": "Crosshair",
        "editable": True,
        "elements": elements,
        "layout": {"kind": "RowsLayout", "spec": {"rows": layout_rows}},
        "links": [],
        "liveNow": False,
        "preferences": {"layout": {"kind": "GridLayout", "spec": {"items": []}}},
        "preload": False,
        "tags": ["monitor", "api"],
        "timeSettings": {
            "autoRefresh": "30s",
            "autoRefreshIntervals": [
                "5s",
                "10s",
                "30s",
                "1m",
                "5m",
                "15m",
                "30m",
                "1h",
                "2h",
                "1d",
            ],
            "fiscalYearStartMonth": 0,
            "from": "now-3h",
            "hideTimepicker": False,
            "timezone": "browser",
            "to": "now",
        },
        "title": "Monitor API",
        "variables": [],
    }

    out = ROOT / "api-dashboard.json"
    out.write_text(json.dumps(dashboard, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out} ({len(elements)} panels)")


if __name__ == "__main__":
    mai