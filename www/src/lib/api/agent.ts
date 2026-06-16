import { apiFetch } from "@/lib/auth/api"

export type AgentLatestVersionResponse = {
  version: string
}

export function getLatestAgentVersion(): Promise<AgentLatestVersionResponse> {
  return apiFetch<AgentLatestVersionResponse>("/v1/agent/latest", { auth: false })
}
