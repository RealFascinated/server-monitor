import type { QueryClient } from "@tanstack/react-query"
import {
  HeadContent,
  Scripts,
  createRootRouteWithContext,
} from "@tanstack/react-router"
import type { ComponentType } from "react"
import { lazy, Suspense } from "react"

import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"
import { NotFoundView } from "@/components/not-found-view"
import { OfflineBanner } from "@/components/offline-banner"
import { AuthProvider } from "@/lib/auth"
import { loadPublicConfigFromEnv } from "@/env/public-config"
import { PublicConfigProvider } from "@/lib/public-config-context"
import { APP_NAME } from "@/lib/page-title"
import { ThemeProvider } from "@/lib/theme"
import { themeInitScript } from "@/lib/theme/script"
import appCss from "../styles.css?url"

const Devtools: ComponentType = import.meta.env.DEV
  ? lazy(() =>
      import("@/components/devtools").then((m) => ({ default: m.Devtools }))
    )
  : () => null

export const Route = createRootRouteWithContext<{
  queryClient: QueryClient
}>()({
  loader: () => ({
    publicConfig: loadPublicConfigFromEnv(),
  }),
  head: () => ({
    meta: [
      {
        charSet: "utf-8",
      },
      {
        name: "viewport",
        content: "width=device-width, initial-scale=1",
      },
      {
        title: APP_NAME,
      },
    ],
    links: [
      {
        rel: "icon",
        href: "/favicon.svg",
        type: "image/svg+xml",
      },
      {
        rel: "stylesheet",
        href: appCss,
      },
    ],
  }),
  notFoundComponent: () => <NotFoundView />,
  shellComponent: RootDocument,
})

function RootDocument({ children }: { children: React.ReactNode }) {
  const { publicConfig } = Route.useLoaderData()

  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <HeadContent />
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body>
        <PublicConfigProvider config={publicConfig}>
          <ThemeProvider>
            <TooltipProvider>
              <OfflineBanner />
              <AuthProvider>{children}</AuthProvider>
              <Toaster richColors closeButton />
            </TooltipProvider>
          </ThemeProvider>
        </PublicConfigProvider>
        {import.meta.env.DEV ? (
          <Suspense fallback={null}>
            <Devtools />
          </Suspense>
        ) : null}
        <Scripts />
      </body>
    </html>
  )
}
