package disk

import "testing"

func TestDedupeMountsByUsage(t *testing.T) {
	mounts := []Mount{
		{Name: "C:", UsedBytes: 1_000, TotalBytes: 2_000},
		{Name: "P:", UsedBytes: 17_000_000_000_000, TotalBytes: 34_000_000_000_000},
		{Name: "Y:", UsedBytes: 17_000_000_000_000, TotalBytes: 34_000_000_000_000},
		{Name: "Z:", UsedBytes: 17_000_000_000_000 + 1024, TotalBytes: 34_000_000_000_000 + 1024},
	}

	got := dedupeMountsByUsage(mounts)
	if len(got) != 2 {
		t.Fatalf("expected 2 mounts after dedupe, got %d: %+v", len(got), got)
	}
	if got[0].Name != "C:" || got[1].Name != "P:" {
		t.Fatalf("unexpected mount order/names: %+v", got)
	}
}

func TestDedupeMountsByUsageKeepsDistinctVolumes(t *testing.T) {
	mounts := []Mount{
		{Name: "C:", UsedBytes: 500, TotalBytes: 1_000},
		{Name: "D:", UsedBytes: 700, TotalBytes: 2_000},
	}

	got := dedupeMountsByUsage(mounts)
	if len(got) != 2 {
		t.Fatalf("expected distinct mounts to remain, got %d", len(got))
	}
}
