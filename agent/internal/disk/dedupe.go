package disk

const usageToleranceBytes uint64 = 16 * 1024 * 1024

// minDedupeTotalBytes avoids collapsing unrelated small volumes that happen to
// differ by less than usageToleranceBytes in absolute terms.
const minDedupeTotalBytes uint64 = 1024 * 1024 * 1024

func dedupeMountsByUsage(mounts []Mount) []Mount {
	if len(mounts) < 2 {
		return mounts
	}

	out := make([]Mount, 0, len(mounts))
	for _, mount := range mounts {
		if mount.TotalBytes == 0 {
			continue
		}
		if isDuplicateMountUsage(mount, out) {
			continue
		}
		out = append(out, mount)
	}
	return out
}

func isDuplicateMountUsage(candidate Mount, existing []Mount) bool {
	for _, mount := range existing {
		if hasSameDiskUsage(mount.UsedBytes, mount.TotalBytes, candidate.UsedBytes, candidate.TotalBytes) {
			return true
		}
	}
	return false
}

func hasSameDiskUsage(usedA, totalA, usedB, totalB uint64) bool {
	if totalA == 0 || totalB == 0 {
		return false
	}
	if totalA < minDedupeTotalBytes || totalB < minDedupeTotalBytes {
		return false
	}
	return withinUsageTolerance(totalA, totalB, usageToleranceBytes) &&
		withinUsageTolerance(usedA, usedB, usageToleranceBytes)
}

func withinUsageTolerance(a, b, tolerance uint64) bool {
	if a >= b {
		return a-b <= tolerance
	}
	return b-a <= tolerance
}
