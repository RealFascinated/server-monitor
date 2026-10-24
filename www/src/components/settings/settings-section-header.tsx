function SettingsSectionHeader({
  title,
  description,
}: {
  title: string
  description: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <h2 className="text-xl font-semibold tracking-tight text-foreground">
        {title}
      </h2>
      <p className="text-sm text-muted-foreground">{description}</p>
    </div>
  )
}

export { SettingsSectionHeader }
