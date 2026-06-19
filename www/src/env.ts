import { createEnv } from "@t3-oss/env-core"
import { z } from "zod"

export const env = createEnv({
  clientPrefix: "VITE_",
  client: {
    VITE_API_URL: z
      .url()
      .default("http://localhost:8080")
      .transform((url) => url.replace(/\/$/, "")),
  },
  runtimeEnv: import.meta.env,
  emptyStringAsUndefined: true,
})
