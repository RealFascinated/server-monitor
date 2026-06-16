type ParsedVersion = {
  major: number
  minor: number
  patch: number
  prerelease: string
}

function parseVersion(raw: string): ParsedVersion | null {
  let trimmed = raw.trim()
  if (trimmed.startsWith("v")) {
    trimmed = trimmed.slice(1)
  }
  if (!trimmed) {
    return null
  }

  let core = trimmed
  let prerelease = ""
  const dash = trimmed.indexOf("-")
  if (dash >= 0) {
    core = trimmed.slice(0, dash)
    prerelease = trimmed.slice(dash + 1)
  }

  const parts = core.split(".")
  if (parts.length < 1 || parts.length > 3) {
    return null
  }

  const major = Number(parts[0])
  const minor = parts.length > 1 ? Number(parts[1]) : 0
  const patch = parts.length > 2 ? Number(parts[2]) : 0
  if (Number.isNaN(major) || Number.isNaN(minor) || Number.isNaN(patch)) {
    return null
  }

  return { major, minor, patch, prerelease }
}

function compareVersions(left: ParsedVersion, right: ParsedVersion): number {
  if (left.major !== right.major) {
    return left.major - right.major
  }
  if (left.minor !== right.minor) {
    return left.minor - right.minor
  }
  if (left.patch !== right.patch) {
    return left.patch - right.patch
  }
  if (!left.prerelease && !right.prerelease) {
    return 0
  }
  if (!left.prerelease) {
    return 1
  }
  if (!right.prerelease) {
    return -1
  }
  return left.prerelease.localeCompare(right.prerelease)
}

export function isVersionOlderThan(current: string, latest: string): boolean {
  const currentVersion = parseVersion(current)
  const latestVersion = parseVersion(latest)
  if (!currentVersion || !latestVersion) {
    return false
  }
  return compareVersions(currentVersion, latestVersion) < 0
}
