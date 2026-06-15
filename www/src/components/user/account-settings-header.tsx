import { User } from "lucide-react"

import { PageHeader } from "@/components/page-header"

function AccountSettingsHeader() {
  return (
    <PageHeader
      sticky
      breadcrumb={[
        { label: "Servers", to: "/" },
        { label: "Account", current: true },
      ]}
      icon={User}
      title="Account"
      description="Your profile and personal preferences."
    />
  )
}

export { AccountSettingsHeader }
