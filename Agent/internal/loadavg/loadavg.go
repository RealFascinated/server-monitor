package loadavg

type Averages struct {
	Load1  float64
	Load5  float64
	Load15 float64
}

func Read() Averages {
	return read()
}
