import { Link } from "@tanstack/react-router"
import { ArrowLeft } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

type NotFoundViewProps = {
  embedded?: boolean
}

function NotFoundView({ embedded = false }: NotFoundViewProps) {
  const content = (
    <>
      <CardHeader className="items-center text-center">
        <CardTitle>Page not found</CardTitle>
        <CardDescription>
          The page you are looking for does not exist or may have been moved.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex justify-center">
        <Button asChild variant="outline">
          <Link to="/">
            <ArrowLeft />
            Back to servers
          </Link>
        </Button>
      </CardContent>
    </>
  )

  if (embedded) {
    return (
      <div className="flex flex-col items-center py-12">
        <Card className="w-full max-w-md">{content}</Card>
      </div>
    )
  }

  return (
    <main className="flex min-h-svh items-center justify-center bg-background px-6 py-8">
      <Card className="w-full max-w-md">{content}</Card>
    </main>
  )
}

export { NotFoundView }
