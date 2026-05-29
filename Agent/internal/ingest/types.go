package ingest

type Data struct {
	AgentVersion     string                  `json:"agentVersion"`
	ServerDetails    ServerDetails           `json:"serverDetails"`
	ServerMetrics    ServerMetrics           `json:"serverMetrics"`
	InterfaceMetrics []InterfaceMetrics      `json:"interfaceMetrics"`
	DiskMetrics      []DiskMetric            `json:"diskMetrics"`
	ZfsArcMetrics    *ZFSArcMetrics          `json:"zfsArcMetrics,omitempty"`
	ZfsPoolMetrics   []ZfsPoolMetric         `json:"zfsPoolMetrics,omitempty"`
	DockerContainers []DockerContainerMetric `json:"dockerContainers,omitempty"`
}

type ServerDetails struct {
	Ip            string  `json:"ip"`
	CoreCount     int     `json:"coreCount"`
	ThreadCount   int     `json:"threadCount"`
	OsName        string  `json:"osName"`
	OsVersion     string  `json:"osVersion"`
	UptimeSeconds int     `json:"uptimeSeconds"`
	CPUModel      string  `json:"cpuModel"`
	SocketCount   int     `json:"socketCount"`
	CPUClockMhz   float64 `json:"cpuClockMhz"`
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
	MemoryBuffers            int64   `json:"memoryBuffers"`
	MemoryCached             int64   `json:"memoryCached"`
	SwapUsed                 int64   `json:"swapUsed"`
	SwapTotal                int64   `json:"swapTotal"`
	ProcessCount             int64   `json:"processCount"`
	RunningProcesses         int64   `json:"runningProcesses"`
	ContextSwitchesPerSecond int64   `json:"contextSwitchesPerSecond"`
	InterruptsPerSecond      int64   `json:"interruptsPerSecond"`
}

type ZFSArcMetrics struct {
	ArcSizeBytes       int64   `json:"arcSizeBytes"`
	ArcTargetBytes     int64   `json:"arcTargetBytes"`
	ArcMaxBytes        int64   `json:"arcMaxBytes"`
	ArcMinBytes        int64   `json:"arcMinBytes"`
	ArcDataBytes       int64   `json:"arcDataBytes"`
	ArcMetadataBytes   int64   `json:"arcMetadataBytes"`
	L2ArcSizeBytes     int64   `json:"l2ArcSizeBytes"`
	ArcHitRatio        float64 `json:"arcHitRatio"`
	ArcMissesPerSecond int64   `json:"arcMissesPerSecond"`
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
	ContainerName string  `json:"containerName"`
	CPUUsage      float64 `json:"cpuUsage"`
	MemoryUsage   int64   `json:"memoryUsage"`
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
