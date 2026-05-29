package metric

func GetClockSpeedMHz() (float64, error) {
	return currentClockSpeedMHz()
}
