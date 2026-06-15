package thermal

import "fascinated.cc/monitor/agent/internal/ingest"

type TemperatureReading struct {
	Sensor  string
	Celsius float64
}

func ToIngest(readings []TemperatureReading) []ingest.TemperatureMetric {
	if len(readings) == 0 {
		return nil
	}
	out := make([]ingest.TemperatureMetric, len(readings))
	for i, reading := range readings {
		out[i] = ingest.TemperatureMetric{
			Sensor:  reading.Sensor,
			Celsius: reading.Celsius,
		}
	}
	return out
}
