import { ThemeSwitcher } from "@/components/theme-switcher"

function AuthPageShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="relative flex min-h-svh items-center justify-center bg-background px-6 py-8">
      <div className="absolute top-4 right-4">
        <ThemeSwitcher />
      </div>
      {children}
    </main>
  )
}

export { AuthPageShell }
