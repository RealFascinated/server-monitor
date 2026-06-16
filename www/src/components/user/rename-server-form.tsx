import { useEffect } from "react"

import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useRenameServer } from "@/hooks/use-rename-server"
import { MAX_SERVER_NAME_LENGTH } from "@/lib/server-name"

type RenameServerFormProps = {
  serverId: number
  currentName: string
}

function RenameServerForm({ serverId, currentName }: RenameServerFormProps) {
  const {
    name,
    setName,
    fieldError,
    setFieldError,
    inputId,
    mutation,
    resetForm,
    submit,
    canSave,
  } = useRenameServer({ serverId, currentName })

  useEffect(() => {
    resetForm()
  }, [currentName, resetForm])

  return (
    <form onSubmit={submit} className="flex max-w-md flex-col gap-2">
      <Label htmlFor={inputId}>Name</Label>
      <div className="flex gap-2">
        <Input
          id={inputId}
          value={name}
          maxLength={MAX_SERVER_NAME_LENGTH}
          onChange={(event) => {
            setName(event.target.value)
            setFieldError(null)
          }}
          aria-invalid={fieldError ? true : undefined}
          disabled={mutation.isPending}
          required
          className="min-w-0 flex-1"
        />
        <Button
          type="submit"
          variant="highlighted"
          size="sm"
          className="shrink-0"
          disabled={!canSave}
        >
          {mutation.isPending ? <Spinner /> : null}
          Save
        </Button>
      </div>
      {fieldError ? (
        <p className="text-xs font-bold text-error">{fieldError}</p>
      ) : null}
    </form>
  )
}

export { RenameServerForm }
