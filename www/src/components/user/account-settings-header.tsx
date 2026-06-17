import { User } from "lucide-react"

import { PageHeader } from "@/components/page-header"

type AccountSettingsHeaderProps = {
  email?: string
}

function AccountSettingsHeader({ email }: AccountSettingsHeaderProps) {
  return (
    <PageHeader
      sticky
      breadcrumb={[
        { label: "Servers", to: "/" },
        { label: "Account", current: true },
      ]}
      icon={User}
      title="Account"
      subtitle={
        email ? (
          <p className="truncate text-sm text-foreground">{email}</p>
        ) : (
          <p className="text-sm text-muted-foreground">
            Your profile, security, and personal preferences.
          </p>
        )
      }
    />
  )
}

export { AccountSettingsHeader }
