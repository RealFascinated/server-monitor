package mdraid

import (
	"strings"
	"testing"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestParse(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name  string
		input string
		want  []ingest.MdArrayMetric
	}{
		{
			name: "healthy raid1",
			input: `Personalities : [raid1]
md0 : active raid1 sdb1[1] sda1[0]
      2095104 blocks super 1.2 [2/2] [UU]

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{{
				Name:        "md0",
				Level:       "raid1",
				DisksTotal:  2,
				DisksActive: 2,
				SyncAction:  "idle",
			}},
		},
		{
			name: "degraded raid5 with failed disk",
			input: `Personalities : [raid6] [raid5] [raid4]
md1 : active raid5 sdb1[1] sdc1[2] sda1[0](F)
      4190208 blocks super 1.2 level 5, 512k chunk, algorithm 2 [3/2] [U_U]

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{{
				Name:        "md1",
				Level:       "raid5",
				DisksTotal:  3,
				DisksActive: 2,
				DisksFailed: 1,
				Degraded:    true,
				SyncAction:  "idle",
			}},
		},
		{
			name: "raid1 recovering",
			input: `Personalities : [raid1]
md2 : active raid1 sdc1[1] sdb1[0]
      2095104 blocks super 1.2 [2/1] [U_]
      [==>.......]  recovery = 12.6% (132/1047552) finish=3.2min speed=48000K/sec

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{{
				Name:              "md2",
				Level:             "raid1",
				DisksTotal:        2,
				DisksActive:       1,
				Degraded:          true,
				SyncAction:        "recovery",
				SyncPercent:       ptrFloat(12.6),
				SyncSpeedKBps:     ptrInt(48000),
				SyncFinishMinutes: ptrFloat(3.2),
			}},
		},
		{
			name: "raid0 has no redundancy counts",
			input: `Personalities : [raid0]
md3 : active raid0 sdd1[0] sde1[1]
      2095104 blocks super 1.2 512k chunks

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{{
				Name:        "md3",
				Level:       "raid0",
				DisksTotal:  2,
				DisksActive: 2,
				SyncAction:  "idle",
			}},
		},
		{
			name: "inactive array",
			input: `Personalities : [raid1]
md4 : inactive sdf1[1](S)
      2095104 blocks super 1.2

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{{
				Name:        "md4",
				DisksTotal:  1,
				DisksActive: 0,
				Degraded:    true,
				SyncAction:  "idle",
			}},
		},
		{
			name: "check with bitmap line and two arrays",
			input: `Personalities : [raid1] [raid6] [raid5] [raid4]
md0 : active raid1 sda1[0] sdb1[1]
      2095104 blocks super 1.2 [2/2] [UU]

md5 : active raid6 sdg1[0] sdh1[1] sdi1[2] sdj1[3]
      8388608 blocks super 1.2 [4/4] [UUUU]
      bitmap: 0/8 pages [0KB], 65536KB chunk
      [====>....]  check = 45.1% (189/418) finish=0.4min speed=200000K/sec

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{
				{
					Name:        "md0",
					Level:       "raid1",
					DisksTotal:  2,
					DisksActive: 2,
					SyncAction:  "idle",
				},
				{
					Name:              "md5",
					Level:             "raid6",
					DisksTotal:        4,
					DisksActive:       4,
					SyncAction:        "check",
					SyncPercent:       ptrFloat(45.1),
					SyncSpeedKBps:     ptrInt(200000),
					SyncFinishMinutes: ptrFloat(0.4),
				},
			},
		},
		{
			name: "reshape",
			input: `Personalities : [raid5]
md6 : active raid5 sdk1[0] sdl1[1] sdm1[2] sdn1[3]
      4190208 blocks super 1.2 level 5, 512k chunk, algorithm 2 [4/4] [UUUU]
      [==>.......]  reshape = 32.5% (34/104) finish=1.5min speed=64000K/sec

unused devices: <none>
`,
			want: []ingest.MdArrayMetric{{
				Name:              "md6",
				Level:             "raid5",
				DisksTotal:        4,
				DisksActive:       4,
				SyncAction:        "reshape",
				SyncPercent:       ptrFloat(32.5),
				SyncSpeedKBps:     ptrInt(64000),
				SyncFinishMinutes: ptrFloat(1.5),
			}},
		},
		{
			name:  "no md arrays",
			input: "Personalities : [raid1]\nunused devices: <none>\n",
			want:  nil,
		},
		{
			name:  "empty",
			input: "",
			want:  nil,
		},
	}

	for _, tt := range tests {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			got := Parse(strings.NewReader(tt.input))
			if len(got) != len(tt.want) {
				t.Fatalf("got %d arrays, want %d: %+v", len(got), len(tt.want), got)
			}
			for i := range tt.want {
				assertMetric(t, got[i], tt.want[i])
			}
		})
	}
}

func assertMetric(t *testing.T, got, want ingest.MdArrayMetric) {
	t.Helper()

	if got.Name != want.Name || got.Level != want.Level ||
		got.DisksTotal != want.DisksTotal || got.DisksActive != want.DisksActive ||
		got.DisksFailed != want.DisksFailed || got.Degraded != want.Degraded ||
		got.SyncAction != want.SyncAction {
		t.Errorf("metric mismatch:\n got %+v\nwant %+v", got, want)
	}
	assertFloatPtr(t, "syncPercent", got.SyncPercent, want.SyncPercent)
	assertIntPtr(t, "syncSpeedKBps", got.SyncSpeedKBps, want.SyncSpeedKBps)
	assertFloatPtr(t, "syncFinishMinutes", got.SyncFinishMinutes, want.SyncFinishMinutes)
}

func assertFloatPtr(t *testing.T, field string, got, want *float64) {
	t.Helper()
	if got == nil || want == nil {
		if got != nil || want != nil {
			t.Errorf("%s: got %v, want %v", field, got, want)
		}
		return
	}
	if *got != *want {
		t.Errorf("%s: got %v, want %v", field, *got, *want)
	}
}

func assertIntPtr(t *testing.T, field string, got, want *int64) {
	t.Helper()
	if got == nil || want == nil {
		if got != nil || want != nil {
			t.Errorf("%s: got %v, want %v", field, got, want)
		}
		return
	}
	if *got != *want {
		t.Errorf("%s: got %v, want %v", field, *got, *want)
	}
}

func ptrFloat(v float64) *float64 { return &v }

func ptrInt(v int64) *int64 { return &v }
