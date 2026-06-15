package cpu

func GetClockSpeedMHz() (float64, error) {
	return currentClockSpeedMHz()
}
