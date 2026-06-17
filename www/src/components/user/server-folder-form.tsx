import { useQuery } from "@tanstack/react-query"
import { useEffect, useId, useState } from "react"

import { FormFieldError } from "@/components/form-field-error"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useMoveServerToFolder } from "@/hooks/use-move-server-to-folder"
import { userServerFoldersQueryOptions } from "@/lib/api/user/folders.queries"
import { MAX_FOLDER_NAME_LENGTH, validateFolderName } from "@/lib/folder-name"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type ServerFolderFormProps = {
  serverId: number
  currentFolderName: string | null
}

function ServerFolderForm({
  serverId,
  currentFolderName,
}: ServerFolderFormProps) {
  const [folderName, setFolderName] = useState(currentFolderName ?? "")
  const [fieldError, setFieldError] = useState<string | null>(null)
  const inputId = useId()
  const listId = useId()

  const { data: folders = [] } = useQuery(userServerFoldersQueryOptions())
  const mutation = useMoveServerToFolder()

  useEffect(() => {
    setFolderName(currentFolderName ?? "")
    setFieldError(null)
  }, [currentFolderName])

  const trimmedName = folderName.trim()
  const isUnchanged =
    trimmedName === (currentFolderName ?? "") ||
    (trimmedName === "" && currentFolderName === null)
  const validationError =
    trimmedName === "" ? null : validateFolderName(folderName)
  const canSave =
    !mutation.isPending && !isUnchanged && validationError === null

  function mutateFolder(nextFolderName: string | null) {
    mutation.mutate(
      { serverId, folderName: nextFolderName },
      {
        onSuccess: () => {
          setFieldError(null)
          toastSuccess("Folder updated")
        },
        onError: (error) => {
          toastMutationError(
            "Could not update folder",
            error,
            "Failed to update folder"
          )
        },
      }
    )
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (trimmedName === "") {
      if (currentFolderName === null) {
        return
      }

      setFieldError(null)
      mutateFolder(null)
      return
    }

    const error = validateFolderName(folderName)
    if (error) {
      setFieldError(error)
      return
    }

    if (trimmedName === currentFolderName) {
      return
    }

    setFieldError(null)
    mutateFolder(trimmedName)
  }

  function handleClear() {
    if (currentFolderName === null) {
      setFolderName("")
      return
    }

    setFieldError(null)
    mutateFolder(null)
  }

  return (
    <form onSubmit={handleSubmit} className="flex max-w-md flex-col gap-2">
      <Label htmlFor={inputId}>Folder</Label>
      <div className="flex gap-2">
        <Input
          id={inputId}
          list={listId}
          value={folderName}
          maxLength={MAX_FOLDER_NAME_LENGTH}
          onChange={(event) => {
            setFolderName(event.target.value)
            setFieldError(null)
          }}
          placeholder="No folder"
          aria-invalid={fieldError ? true : undefined}
          disabled={mutation.isPending}
          className="min-w-0 flex-1"
        />
        <datalist id={listId}>
          {folders.map((folder) => (
            <option key={folder.id} value={folder.name} />
          ))}
        </datalist>
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
      {currentFolderName ? (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="self-start px-0 text-muted-foreground hover:text-foreground"
          disabled={mutation.isPending}
          onClick={handleClear}
        >
          Remove from folder
        </Button>
      ) : null}
      <FormFieldError error={fieldError} />
    </form>
  )
}

export { ServerFolderForm }
