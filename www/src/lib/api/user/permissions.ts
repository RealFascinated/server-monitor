export const ServerPermission = {
  VIEW_SERVER: 1 << 0,
  VIEW_METRICS: 1 << 1,
  LIST_MEMBERS: 1 << 2,
  LIST_INVITES: 1 << 3,
  INVITE_MEMBERS: 1 << 4,
  REMOVE_MEMBERS: 1 << 5,
  REVOKE_INVITES: 1 << 6,
  LEAVE_SERVER: 1 << 7,
  RENAME_SERVER: 1 << 8,
  DELETE_SERVER: 1 << 9,
  ROTATE_INGEST_TOKEN: 1 << 10,
  ASSIGN_FOLDER: 1 << 11,
} as const

export type ServerPermissionBit =
  (typeof ServerPermission)[keyof typeof ServerPermission]

export function hasPermission(
  permissions: number,
  bit: ServerPermissionBit
): boolean {
  return (permissions & bit) !== 0
}
