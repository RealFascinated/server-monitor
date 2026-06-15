import { Settings } from "lucide-react"

import { PageHeader } from "@/components/page-header"

function AdminSettingsHeader() {
  return (
    <PageHeader
      sticky
      breadcrumb={[
        { label: "Servers", to: "/" },
        { label: "Admin Settings", current: true },
      ]}
      icon={Settings}
      title="Admin Settings"
      description="Platform-wide configuration for authentication and registration."
    />
  )
}

export { AdminSettingsHeader }
