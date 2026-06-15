# Monitor Web

Dashboard for Monitor. TanStack Start, React, TypeScript, shadcn/ui.

**Users:** install the full stack with [Docker](../README.md#install-with-docker).

**Developers:**

```bash
bun install
VITE_API_URL=http://localhost:8080 bun dev
```

Runs on port 3000. Point it at a running API (default `http://localhost:8080`).

```bash
docker build -t monitor-www .
```

Image: `ghcr.io/realfascinated/monitor-www`
