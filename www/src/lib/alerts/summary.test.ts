import { describe, expect, it } from "vitest"

import { describeCondition, formatWindow } from "@/lib/alerts/summary"
import {
  alertInputSuffix,
  formatAlertValue,
  fromAlertInputValue,
  toAlertInputValue,
} from "@/lib/alerts/units"

describe("describeCondition", () => {
  it("describes an averaged percent rule", () => {
    expect(
      describeCondition(
        {
          metricLabel: "CPU usage",
          unit: "PERCENT",
          operator: "MORE_THAN",
          threshold: 90,
          windowSeconds: 300,
        },
        "celsius"
      )
    ).toBe("CPU usage, averaged over 5 minutes, is above 90.0%")
  })

  it("shows temperatures in the reader's unit", () => {
    const input = {
      metricLabel: "Temperature",
      unit: "TEMPERATURE" as const,
      operator: "MORE_THAN" as const,
      threshold: 85,
      windowSeconds: 0,
    }

    expect(describeCondition(input, "celsius")).toBe(
      "Temperature is above 85.0 °C"
    )
    expect(describeCondition(input, "fahrenheit")).toBe(
      "Temperature is above 185.0 °F"
    )
  })
})

describe("formatWindow", () => {
  it("uses the largest whole unit", () => {
    expect(formatWindow(0)).toBe("instantly")
    expect(formatWindow(60)).toBe("1 minute")
    expect(formatWindow(1800)).toBe("30 minutes")
    expect(formatWindow(7200)).toBe("2 hours")
    expect(formatWindow(90)).toBe("90 seconds")
  })
})

describe("threshold input conversion", () => {
  it("round-trips temperatures through Fahrenheit", () => {
    const shown = toAlertInputValue("TEMPERATURE", 85, "fahrenheit")
    expect(shown).toBe(185)
    expect(fromAlertInputValue("TEMPERATURE", shown, "fahrenheit")).toBeCloseTo(
      85
    )
    expect(alertInputSuffix("TEMPERATURE", "fahrenheit")).toBe("°F")
  })

  it("enters byte thresholds in megabytes", () => {
    expect(toAlertInputValue("BYTES", 536_870_912, "celsius")).toBe(512)
    expect(fromAlertInputValue("BYTES", 512, "celsius")).toBe(536_870_912)
    expect(formatAlertValue("BYTES", 536_870_912, "celsius")).toBe("512.00 MB")
  })
})
