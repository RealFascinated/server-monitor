import type { ResolvedTheme } from "@/lib/theme/context"

// Distinct hues tuned for the app brand; even spacing avoids muddy near-duplicates.
const CHART_PALETTE_LIGHT = [
  "#7A6FDB", // periwinkle
  "#4A84E6", // blue
  "#3AADBE", // cyan
  "#3FAF87", // emerald
  "#D4A030", // amber
  "#DC6B6B", // coral
  "#D9609F", // rose
  "#8E84D4", // violet
  "#3CA89E", // teal
  "#E08A58", // orange
  "#6F7CE0", // indigo
  "#8AAF5A", // lime
] as const

const CHART_PALETTE_DARK = [
  "#A855F7", // purple
  "#60A5FA", // blue
  "#22D3EE", // cyan
  "#34D399", // emerald
  "#FBBF24", // amber
  "#F87171", // red
  "#F472B6", // rose
  "#C084FC", // violet
  "#2DD4BF", // teal
  "#FB923C", // orange
  "#818CF8", // indigo
  "#A3E635", // lime
] as const

export function getChartColors(theme: ResolvedTheme = "light"): string[] {
  return theme === "dark" ? [...CHART_PALETTE_DARK] : [...CHART_PALETTE_LIGHT]
}

export function getChartColor(
  index: number,
  theme: ResolvedTheme = "light"
): string {
  const palette = getChartColors(theme)
  return palette[index % palette.length]
}
