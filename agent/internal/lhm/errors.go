package lhm

import "errors"

var errUnavailable = errors.New("lhm: helper not available (build with -tags lhmbundle after scripts/build-lhm.ps1)")
