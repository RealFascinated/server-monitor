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

type ServerNotFoundViewProps = {
  serverId?: string
}

function ServerNotFoundView({ serverId }: ServerNotFoundViewProps) {
  return (
    <div className="flex flex-col items-center py-12">
      <Card className="w-full max-w-md">
        <CardHeader className="items-center text-center">
          <CardTitle>Server not found</CardTitle>
          <CardDescription>
            {serverId
              ? `Server ${serverId} does not exist or you do not have access to it.`
              : "This server does not exist or you do not have access to it."}
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
      </Card>
    </div>
  )
}

export { ServerNotFoundView }
