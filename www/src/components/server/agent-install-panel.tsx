import { Check, Copy } from "lucide-react"
import { useState } from "react"

import { Callout } from "@/components/callout"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { SETTINGS_TOOLTIPS } from "@/lib/tooltips/copy"
import {
  getAgentInstallContent,
  getUnraidInstallSteps,
  isCommandInstallMethod,
} from "@/lib/agent/install"
import type { AgentInstallMethod } from "@/lib/agent/install"
import { copyWithToast } from "@/lib/toast"
import { cn } from "@/lib/utils"

const INSTALL_METHODS: {
  value: AgentInstallMethod
  label: string
}[] = [
  { value: "linux", label: "Linux" },
  { value: "windows", label: "Windows" },
  { value: "docker", label: "Docker" },
  { value: "docker-nvidia", label: "Docker (NVIDIA)" },
  { value: "unraid", label: "Unraid" },
  { value: "unraid-nvidia", label: "Unraid (NVIDIA)" },
]

const METHOD_NOTES: Partial<Record<AgentInstallMethod, string>> = {
  windows:
    "Run from an Administrator PowerShell. The agent must run as Administrator so hardware sensors (including GPU power) can be read.",
  docker:
    "Save as docker-compose.yml on the host, then run docker compose up -d. Requires Docker on the host.",
  "docker-nvidia":
    "Use on NVIDIA GPU hosts with the NVIDIA Container Toolkit. Same setup as Docker, but with gpus: all and the NVIDIA image.",
  "unraid-nvidia":
    "Requires the Unraid NVIDIA Driver plugin. Install monitor-agent-nvidia from Community Applications.",
}

type AgentInstallPanelProps = {
  ingestToken: string
}

function CopyableField({
  id,
  label,
  value,
}: {
  id: string
  label: string
  value: string
}) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    const didCopy = await copyWithToast(value)
    if (didCopy) {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  const labelNode = <Label htmlFor={id}>{label}</Label>

  return (
    <div className="flex flex-col gap-2">
      {id === "ingest-token" ? (
        <SimpleTooltip content={SETTINGS_TOOLTIPS.ingestToken}>
          <span className="w-fit cursor-help">{labelNode}</span>
        </SimpleTooltip>
      ) : (
        labelNode
      )}
      <div className="flex gap-2">
        <pre
          id={id}
          className="min-w-0 flex-1 overflow-x-auto rounded-sm border border-border bg-muted px-3 py-2 font-mono text-xs"
        >
          {value}
        </pre>
        <Button
          type="button"
          variant="outline"
          className="shrink-0"
          onClick={() => void handleCopy()}
        >
          {copied ? <Check /> : <Copy />}
          {copied ? "Copied" : "Copy"}
        </Button>
      </div>
    </div>
  )
}

function AgentInstallPanel({ ingestToken }: AgentInstallPanelProps) {
  const [method, setMethod] = useState<AgentInstallMethod>("linux")
  const [copiedCommand, setCopiedCommand] = useState(false)

  const isCommandMethod = isCommandInstallMethod(method)
  const installContent = isCommandMethod
    ? getAgentInstallContent(method, ingestToken)
    : ""
  const unraidSteps =
    method === "unraid"
      ? getUnraidInstallSteps(false)
      : method === "unraid-nvidia"
        ? getUnraidInstallSteps(true)
        : null
  const methodNote = METHOD_NOTES[method]

  async function handleCopyCommand() {
    if (!isCommandMethod) {
      return
    }

    const didCopy = await copyWithToast(installContent)
    if (didCopy) {
      setCopiedCommand(true)
      setTimeout(() => setCopiedCommand(false), 2000)
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-3">
        <CopyableField
          id="ingest-token"
          label="Ingest token"
          value={ingestToken}
        />

        <Callout type="warning" title="Shown once">
          Copy the ingest token now. If you lose it, rotate the ingest token
          again to issue a new one.
        </Callout>
      </div>

      <div className="h-px bg-border" role="separator" />

      <div className="flex flex-col gap-3">
        <Label>Install method</Label>
        <div
          className="inline-flex flex-wrap items-stretch gap-0.5 overflow-hidden rounded-sm border border-border bg-muted/80 p-0.5"
          role="toolbar"
          aria-label="Install method"
        >
          {INSTALL_METHODS.map((option) => {
            const note =
              METHOD_NOTES[option.value] ??
              `Install the Monitor Agent on ${option.label}.`

            return (
              <SimpleTooltip key={option.value} content={note}>
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  className={cn(
                    "h-7 cursor-pointer rounded-sm border-0 px-2.5 text-xs",
                    method === option.value
                      ? "bg-background text-foreground shadow-sm"
                      : "text-muted-foreground hover:bg-background/70"
                  )}
                  onClick={() => setMethod(option.value)}
                >
                  {option.label}
                </Button>
              </SimpleTooltip>
            )
          })}
        </div>

        {methodNote ? (
          <p className="text-xs text-muted-foreground">{methodNote}</p>
        ) : null}
      </div>

      {unraidSteps ? (
        <div className="flex flex-col gap-2">
          <Label>Install steps</Label>
          <ol className="list-decimal space-y-2 pl-5 text-sm text-muted-foreground">
            {unraidSteps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between gap-2">
            <Label htmlFor="install-command">Install command</Label>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-7 shrink-0"
              onClick={() => void handleCopyCommand()}
            >
              {copiedCommand ? <Check /> : <Copy />}
              {copiedCommand ? "Copied" : "Copy"}
            </Button>
          </div>
          <pre
            id="install-command"
            className="max-h-64 overflow-auto rounded-sm border border-border bg-muted p-3 font-mono text-xs whitespace-pre-wrap"
          >
            {installContent}
          </pre>
        </div>
      )}
    </div>
  )
}

export { AgentInstallPanel }
