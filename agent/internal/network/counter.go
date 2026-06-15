package network

type Counter struct {
	Name        string
	BytesRecv   uint64
	BytesSent   uint64
	PacketsRecv uint64
	PacketsSent uint64
	Errin       uint64
	Errout      uint64
}
