import type { ServerResponse } from "@/lib/api/user/servers"

export type ServerFolderGroup = {
  folderName: string
  servers: ServerResponse[]
}

export type ServerFolderLayout = {
  byFolder: Map<string, number[]>
  ungroupedIds: number[]
}

export function partitionServersByFolder(
  servers: ServerResponse[],
  options: { sortServers?: boolean } = {}
): {
  byFolder: Map<string, ServerResponse[]>
  ungrouped: ServerResponse[]
} {
  const byFolder = new Map<string, ServerResponse[]>()
  const ungrouped: ServerResponse[] = []

  for (const server of servers) {
    if (server.folderName) {
      const list = byFolder.get(server.folderName) ?? []
      list.push(server)
      byFolder.set(server.folderName, list)
    } else {
      ungrouped.push(server)
    }
  }

  if (options.sortServers) {
    for (const [folderName, folderServers] of byFolder) {
      byFolder.set(
        folderName,
        folderServers.sort((a, b) => a.serverName.localeCompare(b.serverName))
      )
    }
    ungrouped.sort((a, b) => a.serverName.localeCompare(b.serverName))
  }

  return { byFolder, ungrouped }
}

export function groupServersByFolder(servers: ServerResponse[]): {
  folders: ServerFolderGroup[]
  ungrouped: ServerResponse[]
} {
  const { byFolder, ungrouped } = partitionServersByFolder(servers, {
    sortServers: true,
  })

  const folders = [...byFolder.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([folderName, folderServers]) => ({
      folderName,
      servers: folderServers,
    }))

  return { folders, ungrouped }
}

export function partitionServerIdsByFolder(
  serverIds: number[],
  getServer: (id: number) => ServerResponse | undefined,
  options: { sortServers?: boolean } = {}
): ServerFolderLayout {
  const byFolder = new Map<string, number[]>()
  const ungroupedIds: number[] = []

  for (const serverId of serverIds) {
    const server = getServer(serverId)
    if (!server) {
      continue
    }

    if (server.folderName) {
      const list = byFolder.get(server.folderName) ?? []
      list.push(serverId)
      byFolder.set(server.folderName, list)
    } else {
      ungroupedIds.push(serverId)
    }
  }

  if (options.sortServers) {
    const compareByName = (a: number, b: number) => {
      const nameA = getServer(a)?.serverName ?? ""
      const nameB = getServer(b)?.serverName ?? ""
      return nameA.localeCompare(nameB)
    }

    for (const [folderName, ids] of byFolder) {
      byFolder.set(folderName, [...ids].sort(compareByName))
    }
    ungroupedIds.sort(compareByName)
  }

  return { byFolder, ungroupedIds }
}

function numberArraysEqual(a: number[], b: number[]) {
  return a.length === b.length && a.every((id, index) => id === b[index])
}

export function serverFolderLayoutEquals(
  a: ServerFolderLayout,
  b: ServerFolderLayout
) {
  if (!numberArraysEqual(a.ungroupedIds, b.ungroupedIds)) {
    return false
  }

  if (a.byFolder.size !== b.byFolder.size) {
    return false
  }

  for (const [folderName, ids] of a.byFolder) {
    const otherIds = b.byFolder.get(folderName)
    if (!otherIds || !numberArraysEqual(ids, otherIds)) {
      return false
    }
  }

  return true
}
