//go:build windows

package winpdh

import (
	"fmt"
	"unsafe"

	"golang.org/x/sys/windows"
)

const (
	FmtDouble   = 0x00000200
	InvalidData = 0xc0000bc6
)

var (
	modPdh                          = windows.NewLazySystemDLL("pdh.dll")
	procPdhOpenQuery                = modPdh.NewProc("PdhOpenQuery")
	procPdhAddEnglishCounterW       = modPdh.NewProc("PdhAddEnglishCounterW")
	procPdhCollectQueryData         = modPdh.NewProc("PdhCollectQueryData")
	procPdhGetFormattedCounterValue = modPdh.NewProc("PdhGetFormattedCounterValue")
	procPdhCloseQuery               = modPdh.NewProc("PdhCloseQuery")
)

type counterValue struct {
	CStatus     uint32
	DoubleValue float64
}

type Query struct {
	Handle windows.Handle
}

func OpenQuery() (Query, error) {
	var query windows.Handle
	if r, _, err := procPdhOpenQuery.Call(0, 0, uintptr(unsafe.Pointer(&query))); r != 0 {
		return Query{}, fmt.Errorf("PdhOpenQuery: %w", err)
	}
	return Query{Handle: query}, nil
}

func (q Query) AddCounter(path string) (windows.Handle, error) {
	counterPath, err := windows.UTF16PtrFromString(path)
	if err != nil {
		return 0, err
	}

	var counter windows.Handle
	if r, _, err := procPdhAddEnglishCounterW.Call(
		uintptr(q.Handle),
		uintptr(unsafe.Pointer(counterPath)),
		0,
		uintptr(unsafe.Pointer(&counter)),
	); r != 0 {
		return 0, fmt.Errorf("PdhAddEnglishCounterW(%s): %w", path, err)
	}
	return counter, nil
}

func (q Query) Collect() {
	_, _, _ = procPdhCollectQueryData.Call(uintptr(q.Handle))
}

func ReadDouble(counter windows.Handle) (float64, error) {
	var value counterValue
	r, _, err := procPdhGetFormattedCounterValue.Call(
		uintptr(counter),
		uintptr(FmtDouble),
		0,
		uintptr(unsafe.Pointer(&value)),
	)
	if r != 0 && r != InvalidData {
		return 0, err
	}
	if value.CStatus != 0 && r == InvalidData {
		return 0, fmt.Errorf("pdh counter status 0x%x", value.CStatus)
	}
	return value.DoubleValue, nil
}

func (q Query) Close() {
	if q.Handle != 0 {
		_, _, _ = procPdhCloseQuery.Call(uintptr(q.Handle))
	}
}
