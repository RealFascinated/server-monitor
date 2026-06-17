import { SimpleTooltip } from "@/components/simple-tooltip"
import type { ServerRole } from "@/lib/api/user/servers"
import { formatServerRole } from "@/lib/formatter"
import { SERVER_ROLE_TOOLTIPS } from "@/lib/tooltips/copy"

type RoleTagProps = {
  role: ServerRole | "OWNER"
}

function RoleTag({ role }: RoleTagProps) {
  return (
    <SimpleTooltip content={SERVER_ROLE_TOOLTIPS[role]}>
      <span className="cursor-help bg-muted px-2 py-1 text-xs font-bold text-muted-foreground">
        {formatServerRole(role)}
      </span>
    </SimpleTooltip>
  )
}

export { RoleTag }
