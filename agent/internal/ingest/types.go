package ingest

type Data struct {
	AgentVersion     string                  `json:"agentVersion"`
	// Timestamp is when the sample was collected (unix seconds). Buffered samples are replayed with it.
	Timestamp        int64                   `json:"timestamp,omitempty"`
	ServerDetails    ServerDetails           `json:"serverDetails"`
	ServerMetrics    ServerMetrics           `json:"serverMetrics"`
	InterfaceMetrics []InterfaceMetrics      `json:"interfaceMetrics"`
	DiskMetrics      []DiskMetric            `json:"diskMetrics"`
	ZfsArcMetrics    *ZFSArcMetrics          `json:"zfsArcMetrics,omitempty"`
	PsiMetrics           *PsiMetrics             `json:"psiMetrics,omitempty"`
	ZfsPoolMetrics   []ZfsPoolMetric         `json:"zfsPoolMetrics,omitempty"`
	DockerContainers     []DockerContainerMetric `json:"dockerContainers,omitempty"`
	GPUMetrics           []GPUMetric             `json:"gpuMetrics,omitempty"`
	TCPConnectionMetrics []TCPConnectionMetric   `json:"tcpConnectionMetrics,omitempty"`
	AgentMetrics         *AgentMetrics           `json:"agentMetrics,omitempty"`
	SmartMetrics         []SmartMetric           `json:"smartMetrics,omitempty"`
}

// SmartMetric is the health of one physical disk as reported by smartctl. Fields that do not
// apply to the disk's type (SATA attributes on NVMe and the reverse) are omitted.
type SmartMetric struct {
	Device string `json:"device"`
	Model  string `json:"model"`

	HealthPassed       *bool    `json:"healthPassed,omitempty"`
	TemperatureCelsius *float64 `json:"temperatureCelsius,omitempty"`
	PowerOnHours       *int64   `json:"powerOnHours,omitempty"`

	ReallocatedSectors   *int64 `json:"reallocatedSectors,omitempty"`
	ReallocatedEvents    *int64 `json:"reallocatedEvents,omitempty"`
	PendingSectors       *int64 `json:"pendingSectors,omitempty"`
	ReportedUncorrect    *int64 `json:"reportedUncorrect,omitempty"`
	OfflineUncorrectable *int64 `json:"offlineUncorrectable,omitempty"`
	UdmaCrcErrors        *int64 `json:"udmaCrcErrors,omitempty"`

	LoadCycleCount       *int64 `json:"loadCycleCount,omitempty"`
	StartStopCounts      *int64 `json:"startStopCounts,omitempty"`
	PowerCycleCount      *int64 `json:"powerCycleCount,omitempty"`
	PowerOffRetracts     *int64 `json:"powerOffRetracts,omitempty"`
	HeliumPercent        *int64 `json:"heliumPercent,omitempty"`
	ErrorLogEntries      *int64 `json:"errorLogEntries,omitempty"`
	EnduranceUsedPercent *int64 `json:"enduranceUsedPercent,omitempty"`

	PercentageUsed   *int64 `json:"percentageUsed,omitempty"`
	AvailableSpare   *int64 `json:"availableSpare,omitempty"`
	MediaErrors      *int64 `json:"mediaErrors,omitempty"`
	UnsafeShutdowns  *int64 `json:"unsafeShutdowns,omitempty"`
	DataWrittenBytes *int64 `json:"dataWrittenBytes,omitempty"`
	DataReadBytes    *int64 `json:"dataReadBytes,omitempty"`
}

// AgentMetrics describes the agent process itself (resource usage and push health), shown to admins in fleet metrics.
type AgentMetrics struct {
	RssBytes             int64 `json:"rssBytes"`
	Goroutines           int64 `json:"goroutines"`
	HeapInUseBytes       int64 `json:"heapInUseBytes"`
	OpenFds              int64 `json:"openFds"`
	UptimeSeconds        int64 `json:"uptimeSeconds"`
	LastPushDurationMs   int64 `json:"lastPushDurationMs"`
	LastPushPayloadBytes int64 `json:"lastPushPayloadBytes"`
	PushFailuresTotal    int64 `json:"pushFailuresTotal"`

	BufferedSamples        int64 `json:"bufferedSamples"`
	BufferedBytes          int64 `json:"bufferedBytes"`
	BufferOldestAgeSeconds int64 `json:"bufferOldestAgeSeconds"`
	BufferDroppedTotal     int64 `json:"bufferDroppedTotal"`
}

type GPUMetric struct {
	DeviceID              string  `json:"deviceId"`
	Name                  string  `json:"name"`
	Vendor                string  `json:"vendor"`
	UsagePercent          float64  `json:"usagePercent"`
	EncoderUsagePercent   *float64 `json:"encoderUsagePercent,omitempty"`
	DecoderUsagePercent   *float64 `json:"decoderUsagePercent,omitempty"`
	MemoryUsedBytes       int64   `json:"memoryUsedBytes"`
	MemoryTotalBytes      int64   `json:"memoryTotalBytes"`
	TemperatureCelsius    float64 `json:"temperatureCelsius"`
	PowerWatts            float64  `json:"powerWatts"`
}

func FloatPtr(v float64) *float64 {
	return &v
}

type ServerDetails struct {
	Ip            string  `json:"ip"`
	CoreCount     int     `json:"coreCount"`
	ThreadCount   int     `json:"threadCount"`
	OsName        string  `json:"osName"`
	OsVersion     string  `json:"osVersion"`
	KernelVersion string  `json:"kernelVersion"`
	UptimeSeconds int     `json:"uptimeSeconds"`
	CPUModel      string  `json:"cpuModel"`
	SocketCount   int     `json:"socketCount"`
	CPUClockMhz   float64 `json:"cpuClockMhz"`
}

type CPUCoreMetric struct {
	CPU          string  `json:"cpu"`
	UsagePercent float64 `json:"usagePercent"`
}

type TemperatureMetric struct {
	Sensor  string  `json:"sensor"`
	Celsius float64 `json:"celsius"`
}

type TCPConnectionMetric struct {
	State string `json:"state"`
	Count int64  `json:"count"`
}

type ServerMetrics struct {
	CPUUsage                 float64 `json:"cpuUsage"`
	MemoryUsage              float64 `json:"memoryUsage"`
	MemoryAvailable          float64 `json:"memoryAvailable"`
	MemoryTotal              float64 `json:"memoryTotal"`
	Load1                    float64 `json:"load1"`
	Load5                    float64 `json:"load5"`
	Load15                   float64 `json:"load15"`
	CPUUserPercent           float64 `json:"cpuUserPercent"`
	CPUSystemPercent         float64 `json:"cpuSystemPercent"`
	CPUIowaitPercent         float64 `json:"cpuIowaitPercent"`
	CPUStealPercent          float64 `json:"cpuStealPercent"`
	CPUPowerWatts            float64 `json:"cpuPowerWatts,omitempty"`
	MemoryBuffers            int64   `json:"memoryBuffers"`
	MemoryCached             int64   `json:"memoryCached"`
	SwapUsed                 int64   `json:"swapUsed"`
	SwapTotal                int64   `json:"swapTotal"`
	ProcessCount             int64   `json:"processCount"`
	RunningProcesses         int64   `json:"runningProcesses"`
	ContextSwitchesPerSecond int64   `json:"contextSwitchesPerSecond"`
	InterruptsPerSecond      int64   `json:"interruptsPerSecond"`
	FdOpen                   int64   `json:"fdOpen"`
	FdMax                    int64   `json:"fdMax,omitempty"`
	FdUsagePercent           float64 `json:"fdUsagePercent,omitempty"`
	BatteryPercent           *float64 `json:"batteryPercent,omitempty"`
	OomKillsTotal            int64   `json:"oomKillsTotal"`
	OomKillsPerSecond        int64   `json:"oomKillsPerSecond"`
	ClockSynced              *bool    `json:"clockSynced,omitempty"`
	ClockErrorMs             *float64 `json:"clockErrorMs,omitempty"`
	ClockOffsetMs            *float64 `json:"clockOffsetMs,omitempty"`
	CPUCoreMetrics           []CPUCoreMetric      `json:"cpuCoreMetrics,omitempty"`
	TemperatureMetrics       []TemperatureMetric  `json:"temperatureMetrics,omitempty"`
}

type ZFSArcMetrics struct {
	ArcSizeBytes       int64   `json:"arcSizeBytes"`
	ArcTargetBytes     int64   `json:"arcTargetBytes"`
	ArcMaxBytes        int64   `json:"arcMaxBytes"`
	ArcMinBytes        int64   `json:"arcMinBytes"`
	ArcDataBytes       int64   `json:"arcDataBytes"`
	ArcMetadataBytes   int64   `json:"arcMetadataBytes"`
	L2ArcSizeBytes     int64   `json:"l2arcSizeBytes"`
	ArcHitRatio        float64 `json:"arcHitRatio"`
	ArcMissesPerSecond int64   `json:"arcMissesPerSecond"`
}

// PsiMetrics is Linux Pressure Stall Information: how much wall-clock time tasks spent stalled
// waiting on a resource. Avg* are kernel moving averages of the share of time the resource was
// stalled, over 10 seconds, 1 minute and 5 minutes; "some" means at least one task was stalled
// and "full" means every non-idle task was. TotalMs is cumulative stall time since boot.
// Unset when PSI is unavailable (no CONFIG_PSI, or PSI disabled on the kernel command line), and
// full is unset for CPU on kernels that export only a some line for it.
type PsiMetrics struct {
	CPUSomeAvg10Pct   *float64 `json:"cpuSomeAvg10Pct,omitempty"`
	CPUSomeAvg60Pct   *float64 `json:"cpuSomeAvg60Pct,omitempty"`
	CPUSomeAvg300Pct  *float64 `json:"cpuSomeAvg300Pct,omitempty"`
	CPUSomeTotalMs    *float64 `json:"cpuSomeTotalMs,omitempty"`
	CPUFullAvg10Pct   *float64 `json:"cpuFullAvg10Pct,omitempty"`
	CPUFullAvg60Pct   *float64 `json:"cpuFullAvg60Pct,omitempty"`
	CPUFullAvg300Pct  *float64 `json:"cpuFullAvg300Pct,omitempty"`
	CPUFullTotalMs    *float64 `json:"cpuFullTotalMs,omitempty"`
	MemorySomeAvg10Pct  *float64 `json:"memorySomeAvg10Pct,omitempty"`
	MemorySomeAvg60Pct  *float64 `json:"memorySomeAvg60Pct,omitempty"`
	MemorySomeAvg300Pct *float64 `json:"memorySomeAvg300Pct,omitempty"`
	MemorySomeTotalMs   *float64 `json:"memorySomeTotalMs,omitempty"`
	MemoryFullAvg10Pct  *float64 `json:"memoryFullAvg10Pct,omitempty"`
	MemoryFullAvg60Pct  *float64 `json:"memoryFullAvg60Pct,omitempty"`
	MemoryFullAvg300Pct *float64 `json:"memoryFullAvg300Pct,omitempty"`
	MemoryFullTotalMs   *float64 `json:"memoryFullTotalMs,omitempty"`
	IOSomeAvg10Pct      *float64 `json:"ioSomeAvg10Pct,omitempty"`
	IOSomeAvg60Pct      *float64 `json:"ioSomeAvg60Pct,omitempty"`
	IOSomeAvg300Pct     *float64 `json:"ioSomeAvg300Pct,omitempty"`
	IOSomeTotalMs       *float64 `json:"ioSomeTotalMs,omitempty"`
	IOFullAvg10Pct      *float64 `json:"ioFullAvg10Pct,omitempty"`
	IOFullAvg60Pct      *float64 `json:"ioFullAvg60Pct,omitempty"`
	IOFullAvg300Pct     *float64 `json:"ioFullAvg300Pct,omitempty"`
	IOFullTotalMs       *float64 `json:"ioFullTotalMs,omitempty"`
}

type InterfaceMetrics struct {
	InterfaceName      string `json:"interfaceName"`
	RxBytesPerSecond   int64  `json:"rxBytesPerSecond"`
	TxBytesPerSecond   int64  `json:"txBytesPerSecond"`
	RxPacketsPerSecond int64  `json:"rxPacketsPerSecond"`
	TxPacketsPerSecond int64  `json:"txPacketsPerSecond"`
	RxErrorsPerSecond  int64  `json:"rxErrorsPerSecond"`
	TxErrorsPerSecond  int64  `json:"txErrorsPerSecond"`
}

type ZfsPoolMetric struct {
	PoolName             string  `json:"poolName"`
	Health               string  `json:"health"`
	CapacityPercent      float64 `json:"capacityPercent"`
	AllocatedBytes       int64   `json:"allocatedBytes"`
	FreeBytes            int64   `json:"freeBytes"`
	TotalBytes           int64   `json:"totalBytes"`
	FragmentationPercent float64 `json:"fragmentationPercent"`
	ScanState            string  `json:"scanState"`
	ScanPercent          float64 `json:"scanPercent"`
	ReadBps              int64   `json:"readBps"`
	WriteBps             int64   `json:"writeBps"`
	ReadIops             int64   `json:"readIops"`
	WriteIops            int64   `json:"writeIops"`
	ChecksumErrors       int64   `json:"checksumErrors"`
}

type DockerContainerMetric struct {
	ContainerName string   `json:"containerName"`
	CPUUsage      *float64 `json:"cpuUsage,omitempty"`
	MemoryUsage   int64    `json:"memoryUsage"`

	// Rates are omitted when unknown (first sample, counter reset, host networking, CLI fallback).
	NetworkRxBytesPerSecond  *int64 `json:"networkRxBytesPerSecond,omitempty"`
	NetworkTxBytesPerSecond  *int64 `json:"networkTxBytesPerSecond,omitempty"`
	BlockReadBytesPerSecond  *int64 `json:"blockReadBytesPerSecond,omitempty"`
	BlockWriteBytesPerSecond *int64 `json:"blockWriteBytesPerSecond,omitempty"`
}

type DiskMetric struct {
	DiskName              string  `json:"diskName"`
	UsedBytes             int64   `json:"usedBytes"`
	TotalBytes            int64   `json:"totalBytes"`
	IoReadBytesPerSecond  int64   `json:"ioReadBytesPerSecond"`
	IoWriteBytesPerSecond int64   `json:"ioWriteBytesPerSecond"`
	IoUsagePercent        float64 `json:"ioUsagePercent"`
	IoWaitMilliseconds    float64 `json:"ioWaitMilliseconds"`
	InodeUsed             int64   `json:"inodeUsed"`
	InodeTotal            int64   `json:"inodeTotal"`
	ReadIops              int64   `json:"readIops"`
	WriteIops             int64   `json:"writeIops"`
	ReadLatencyMs         int64   `json:"readLatencyMs"`
	WriteLatencyMs        int64   `json:"writeLatencyMs"`
}
