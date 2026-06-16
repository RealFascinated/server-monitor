import { Link } from "@tanstack/react-router"

function AuthPageFooter({ children }: { children: React.ReactNode }) {
  return (
    <p className="mt-6 border-t border-border pt-4 text-center text-sm text-balance text-muted-foreground">
      {children}
    </p>
  )
}

type AuthPageLinkProps = {
  to: string
  children: React.ReactNode
}

function AuthPageLink({ to, children }: AuthPageLinkProps) {
  return (
    <Link
      to={to}
      className="font-medium text-monitor underline-offset-4 hover:underline dark:text-warning"
    >
      {children}
    </Link>
  )
}

export { AuthPageFooter, AuthPageLink }
