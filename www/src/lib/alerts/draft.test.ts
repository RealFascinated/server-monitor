import { describe, expect, it } from "vitest"

import {
  EMPTY_DRAFT,
  draftForMetric,
  draftToRequest,
  ruleToCopyRequest,
  ruleToDraft,
  validateDraft,
} from "@/lib/alerts/draft"
import type { AlertMetric, AlertRule } from "@/lib/api/user/alerts"

const cpu: AlertMetric = {
  metric: "monitor_host_cpu_usage",
  label: "CPU usage",
  unit: "PERCENT",
  defaultOperator: "MORE_THAN",
  defaultThreshold: 90,
  group: "CPU",
  labelKeys: [],
}

const temperature: AlertMetric = {
  metric: "monitor_host_temperature_celsius",
  label: "Temperature",
  unit: "TEMPERATURE",
  defaultOperator: "MORE_THAN",
  defaultThreshold: 80,
  group: "Hardware",
  labelKeys: ["sensor"],
}

describe("draftForMetric", () => {
  it("prefills the metric's suggested condition and clears the label filter", () => {
    const draft = draftForMetric(
      { ...EMPTY_DRAFT, labelFilter: { disk: "/" }, windowSeconds: 300 },
      cpu,
      "celsius"
    )

    expect(draft.metric).toBe(cpu.metric)
    expect(draft.threshold).toBe("90")
    expect(draft.labelFilter).toEqual({})
    expect(draft.windowSeconds).toBe(300)
  })

  it("shows the suggested temperature in the reader's unit", () => {
    expect(
      draftForMetric(EMPTY_DRAFT, temperature, "fahrenheit").threshold
    ).toBe("176")
  })

  it("never averages server up", () => {
    const up: AlertMetric = {
      ...cpu,
      metric: "monitor_server_up",
      unit: "STATE",
    }
    expect(
      draftForMetric({ ...EMPTY_DRAFT, windowSeconds: 300 }, up, "celsius")
        .windowSeconds
    ).toBe(0)
  })
})

describe("validateDraft", () => {
  const valid = { ...EMPTY_DRAFT, metric: cpu.metric, threshold: "90" }

  it("accepts a complete draft", () => {
    expect(validateDraft(valid, "PERCENT", "celsius")).toEqual({})
  })

  it("requires a metric and a numeric threshold", () => {
    const errors = validateDraft(EMPTY_DRAFT, undefined, "celsius")
    expect(errors.metric).toBeDefined()
    expect(errors.threshold).toBeDefined()
    expect(
      validateDraft({ ...valid, threshold: "abc" }, "PERCENT", "celsius")
        .threshold
    ).toBeDefined()
  })

  it("keeps the recovery threshold on the correct side", () => {
    expect(
      validateDraft({ ...valid, recoveryThreshold: "95" }, "PERCENT", "celsius")
        .recoveryThreshold
    ).toBeDefined()
    expect(
      validateDraft({ ...valid, recoveryThreshold: "80" }, "PERCENT", "celsius")
    ).toEqual({})
    expect(
      validateDraft(
        { ...valid, operator: "LESS_THAN", recoveryThreshold: "80" },
        "PERCENT",
        "celsius"
      ).recoveryThreshold
    ).toBeDefined()
  })
})

describe("draftToRequest", () => {
  it("stores temperatures in Celsius and turns blanks into nulls", () => {
    const request = draftToRequest(
      7,
      { ...EMPTY_DRAFT, metric: temperature.metric, threshold: "176" },
      "TEMPERATURE",
      "fahrenheit"
    )

    expect(request.groupId).toBe(7)
    expect(request.threshold).toBeCloseTo(80)
    expect(request.recoveryThreshold).toBeNull()
    expect(request.repeatIntervalSeconds).toBeNull()
    expect(request.name).toBeNull()
  })
})

describe("ruleToDraft", () => {
  it("round-trips through the request", () => {
    const rule: AlertRule = {
      id: 1,
      groupId: 7,
      groupName: "Prod",
      name: "High CPU",
      metric: cpu.metric,
      metricLabel: cpu.label,
      unit: "PERCENT",
      labelFilter: {},
      operator: "MORE_THAN",
      threshold: 90,
      recoveryThreshold: 80,
      windowSeconds: 300,
      severity: "CRITICAL",
      repeatIntervalSeconds: 3600,
      enabled: true,
      createdAt: "2026-01-01T00:00:00Z",
      firingCount: 0,
      silencedCount: 0,
    }

    const request = draftToRequest(
      7,
      ruleToDraft(rule, "celsius"),
      "PERCENT",
      "celsius"
    )

    expect(request).toMatchObject({
      groupId: 7,
      name: "High CPU",
      threshold: 90,
      recoveryThreshold: 80,
      windowSeconds: 300,
      severity: "CRITICAL",
      repeatIntervalSeconds: 3600,
    })
  })
})

describe("ruleToCopyRequest", () => {
  it("copies the condition, marks the name and starts disabled", () => {
    const copy = ruleToCopyRequest({
      id: 1,
      groupId: 7,
      groupName: "Prod",
      name: "High CPU",
      metric: cpu.metric,
      metricLabel: cpu.label,
      unit: "PERCENT",
      labelFilter: { disk: "/" },
      operator: "MORE_THAN",
      threshold: 90,
      recoveryThreshold: null,
      windowSeconds: 300,
      severity: "WARNING",
      repeatIntervalSeconds: null,
      enabled: true,
      createdAt: "2026-01-01T00:00:00Z",
      firingCount: 2,
      silencedCount: 0,
    })

    expect(copy).toMatchObject({
      groupId: 7,
      name: "High CPU (copy)",
      labelFilter: { disk: "/" },
      threshold: 90,
      enabled: false,
    })
  })
})
