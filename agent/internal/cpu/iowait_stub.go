//go:build !windows

package cpu

func BeginIowaitSample() {}

func EndIowaitSample() float64 { return 0 }
