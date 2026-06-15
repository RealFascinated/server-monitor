import { createEnv } from "@t3-oss/env-core"
import { z } from "zod"

export const env = createEnv({
  clientPrefix: "VITE_",
  client: {
    VITE_API_URL: z.string().default(""),
  },
  runtimeEnv: import.meta.env,
  emptyStringAsUndefined: true,
  skipValidation: import.meta.env.SKIP_ENV_VALIDATION === "true",
})
