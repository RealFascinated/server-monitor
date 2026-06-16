import { readCssVar } from "@/lib/css-vars"
import type { ResolvedTheme } from "@/lib/theme/context"

export const CHART_COLOR_VARS = [
  "--chart-1",
  "--chart-2",
  "--chart-3",
  "--chart-4",
  "--chart-5",
  "--chart-6",
  "--chart-7",
  "--chart-8",
  "--chart-9",
  "--chart-10",
  "--chart-11",
  "--chart-12",
] as const

const CHART_PALETTE_LIGHT = [
  "#7A6FDB",
  "#4A84E6",
  "#3AADBE",
  "#3FAF87",
  "#D4A030",
  "#DC6B6B",
  "#D9609F",
  "#8E84D4",
  "#3CA89E",
  "#E08A58",
  "#6F7CE0",
  "#8AAF5A",
] as const

const CHART_PALETTE_DARK = [
  "#A855F7",
  "#60A5FA",
  "#22D3EE",
  "#34D399",
  "#FBBF24",
  "#F87171",
  "#F472B6",
  "#C084FC",
  "#2DD4BF",
  "#FB923C",
  "#818CF8",
  "#A3E635",
] as const

function fallbackPalette(theme: ResolvedTheme): readonly string[] {
  return theme === "dark" ? CHART_PALETTE_DARK : CHART_PALETTE_LIGHT
}

export function getChartColors(theme: ResolvedTheme = "light"): string[] {
  const fallbacks = fallbackPalette(theme)

  return CHART_COLOR_VARS.map((cssVar, index) =>
    readCssVar(cssVar, fallbacks[index])
  )
}

export function getChartColor(
  index: number,
  theme: ResolvedTheme = "light"
): string {
  const palette = getChartColors(theme)
  return palette[index % palette.length]
}
