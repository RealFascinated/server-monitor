function SettingsSectionHeader({
  title,
  description,
}: {
  title: string
  description: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <h2 className="text-xl font-bold dark:text-white">{title}</h2>
      <p className="text-xs font-bold text-neutral-500">{description}</p>
    </div>
  )
}

export { SettingsSectionHeader }
