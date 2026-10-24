//go:build windows

package selfstats

import (
	"os"
	"unsafe"

	"github.com/shirou/gopsutil/v4/process"
	"golang.org/x/sys/windows"
)

func rssBytes() int64 {
	proc, err := process.NewProcess(int32(os.Getpid()))
	if err != nil {
		return 0
	}
	info, err := proc.MemoryInfo()
	if err != nil || info == nil {
		return 0
	}
	return int64(info.RSS)
}

var procGetProcessHandleCount = windows.NewLazySystemDLL("kernel32.dll").NewProc("GetProcessHandleCount")

func openFDs() int64 {
	var handles uint32
	ret, _, _ := procGetProcessHandleCount.Call(uintptr(windows.CurrentProcess()), uintptr(unsafe.Pointer(&handles)))
	if ret == 0 {
		return 0
	}
	return int64(handles)
}
