//go:build !windows

package metric

func BeginIowaitSample() {}

func EndIowaitSample() float64 { return 0 }
