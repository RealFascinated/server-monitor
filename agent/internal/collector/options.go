package collector

type Options struct {
	HasZFS       bool
	EnableDocker bool
	EnableGPU    bool

	EnableTCP               bool
	EnableThermal           bool
	EnableCPUCores          bool
	EnableDockerCLIFallback bool
	EnableBattery           bool
	EnableTimesync          bool
	EnableSmart             bool
}
