import { createFileRoute } from "@tanstack/react-router"

import { loadPublicConfigFromEnv } from "@/env/public-config"

export const Route = createFileRoute("/config.json")({
  server: {
    handlers: {
      GET: () =>
        Response.json(loadPublicConfigFromEnv(), {
          headers: { "Cache-Control": "no-store" },
        }),
    },
  },
})
