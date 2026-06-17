import { AgentInstallPanel } from "@/components/server/agent-install-panel"
import { WaitingForServerData } from "@/components/server/waiting-for-server-data"

type AgentSetupPanelProps = {
  serverId: number
  ingestToken: string
}

function AgentSetupPanel({ serverId, ingestToken }: AgentSetupPanelProps) {
  return (
    <>
      <WaitingForServerData serverId={serverId} />
      <AgentInstallPanel ingestToken={ingestToken} />
    </>
  )
}

export { AgentSetupPanel }
