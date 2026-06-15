//go:build linux

package gpu

import "testing"

func TestParseLspciLineAMDGPU(t *testing.T) {
	line := "0000:0c:00.0 VGA compatible controller [0300]: Advanced Micro Devices, Inc. [AMD/ATI] Navi 31 [Radeon RX 7900 XT/7900 XTX/7900 GRE/7900M] [1002:744c] (rev c8)"
	slot, name := parseLspciLine(line)
	if slot != "0000:0c:00.0" {
		t.Fatalf("slot: %q", slot)
	}
	want := "Advanced Micro Devices, Inc. [AMD/ATI] Navi 31 [Radeon RX 7900 XT/7900 XTX/7900 GRE/7900M]"
	if name != want {
		t.Fatalf("name: %q", name)
	}
}

func TestParseLspciLineIntelGPU(t *testing.T) {
	line := `0000:00:02.0 VGA compatible controller [0300]: Intel Corporation Raptor Lake-S GT1 [UHD Graphics 770] [8086:a780] (rev 04)`
	slot, name := parseLspciLine(line)
	if slot != "0000:00:02.0" {
		t.Fatalf("slot: %q", slot)
	}
	want := "Intel Corporation Raptor Lake-S GT1 [UHD Graphics 770]"
	if name != want {
		t.Fatalf("name: %q", name)
	}
}
