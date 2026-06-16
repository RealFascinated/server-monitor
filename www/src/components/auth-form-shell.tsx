import { Callout } from "@/components/callout"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

type AuthFormShellProps = {
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void
  isPending: boolean
  submitLabel: React.ReactNode
  apiError?: string | null
  apiErrorTitle?: string
  className?: string
  submitClassName?: string
  children: React.ReactNode
}

function AuthFormShell({
  onSubmit,
  isPending,
  submitLabel,
  apiError,
  apiErrorTitle,
  className,
  submitClassName,
  children,
}: AuthFormShellProps) {
  return (
    <form
      className={cn("flex flex-col gap-4", className)}
      onSubmit={onSubmit}
    >
      {apiError ? (
        <Callout type="danger" title={apiErrorTitle ?? "Request failed"}>
          {apiError}
        </Callout>
      ) : null}
      {children}
      <Button
        type="submit"
        variant="highlighted"
        disabled={isPending}
        className={submitClassName}
      >
        {isPending ? <Spinner /> : null}
        {submitLabel}
      </Button>
    </form>
  )
}

type AuthFormFieldProps = {
  id: string
  label: React.ReactNode
  error?: string
  children: React.ReactNode
}

function AuthFormField({ id, label, error, children }: AuthFormFieldProps) {
  return (
    <div className="flex flex-col gap-2">
      {typeof label === "string" ? (
        <Label htmlFor={id}>{label}</Label>
      ) : (
        label
      )}
      {children}
      {error ? (
        <p className="text-xs font-bold text-error">{error}</p>
      ) : null}
    </div>
  )
}

export { AuthFormField, AuthFormShell }
