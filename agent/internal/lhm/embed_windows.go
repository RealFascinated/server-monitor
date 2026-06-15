//go:build windows && lhmbundle

package lhm

import "embed"

// Built by scripts/build-lhm.ps1 before GOOS=windows go build.
//
//go:embed all:sensorhost/bin/Release/net48
var embeddedSensorHost embed.FS
