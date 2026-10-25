import { Cpu, HardDrive, MemoryStick, Power, Thermometer } from "lucide-react"
import type { LucideIcon } from "lucide-react"

export type AlertTemplate = {
  id: string
  title: string
  description: string
  icon: LucideIcon
  /** Backend metric name; the builder fills in the suggested condition for it. */
  metric: string
}

/** Common starting points. Each just preselects a metric, so thresholds come from the backend's own defaults. */
export const ALERT_TEMPLATES: AlertTemplate[] = [
  {
    id: "cpu",
    title: "High CPU",
    description: "CPU usage stays high",
    icon: Cpu,
    metric: "monitor_host_cpu_usage",
  },
  {
    id: "memory",
    title: "Low memory",
    description: "Memory usage stays high",
    icon: MemoryStick,
    metric: "monitor_host_mem_usage_pct",
  },
  {
    id: "disk",
    title: "Disk filling up",
    description: "A disk is almost full",
    icon: HardDrive,
    metric: "monitor_disk_usage_percent",
  },
  {
    id: "temperature",
    title: "Running hot",
    description: "A sensor reports high temperature",
    icon: Thermometer,
    metric: "monitor_host_temperature_celsius",
  },
  {
    id: "offline",
    title: "Server offline",
    description: "The agent stops reporting",
    icon: Power,
    metric: "monitor_server_up",
  },
]
