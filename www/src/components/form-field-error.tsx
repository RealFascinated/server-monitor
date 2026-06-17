type FormFieldErrorProps = {
  error?: string | null
}

function FormFieldError({ error }: FormFieldErrorProps) {
  if (!error) {
    return null
  }

  return <p className="text-xs font-bold text-error">{error}</p>
}

export { FormFieldError }
