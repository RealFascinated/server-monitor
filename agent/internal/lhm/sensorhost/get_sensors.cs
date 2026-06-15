#nullable enable
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using LibreHardwareMonitor.Hardware;
using LibreHardwareMonitor.Hardware.Gpu;

namespace MonitorAgent.Lhm;

internal sealed class UpdateVisitor : IVisitor
{
    public void VisitComputer(IComputer computer) => computer.Traverse(this);
    public void VisitHardware(IHardware hardware)
    {
        hardware.Update();
        foreach (var sub in hardware.SubHardware)
            sub.Accept(this);
    }
    public void VisitSensor(ISensor sensor) { }
    public void VisitParameter(IParameter parameter) { }
}

internal sealed class ServerMetricsSnapshot
{
    public double? CpuTotalPercent;
    public double? CpuPowerWatts;
    public List<CoreEntry> Cores = new();
    public MemoryEntry Memory = new();
    public List<TemperatureEntry> Temperatures = new();
    public List<GpuEntry> Gpus = new();
}

internal sealed class GpuEntry
{
    public string DeviceId = "";
    public string Name = "";
    public string Vendor = "";
    public double? UsagePercent;
    public double EncoderUsagePercent;
    public double DecoderUsagePercent;
    public long? MemoryUsedBytes;
    public long? MemoryTotalBytes;
    public double? TemperatureCelsius;
    public double? PowerWatts;
    public int PowerPriority = -1;
    public int UsagePriority = -1;
    public int MemoryTotalPriority = -1;
    public int MemoryUsedPriority = -1;
}

internal sealed class CoreEntry
{
    public string Cpu = "";
    public double UsagePercent;
}

internal sealed class MemoryEntry
{
    public long? Used;
    public long? Available;
    public long? Total;
}

internal sealed class TemperatureEntry
{
    public string Sensor = "";
    public double Celsius;
}

internal static class Program
{
    private static Computer? _computer;
    private static readonly UpdateVisitor Visitor = new();

    static void Main()
    {
        _computer = new Computer
        {
            IsCpuEnabled = true,
            IsGpuEnabled = true,
            IsMemoryEnabled = true,
            IsMotherboardEnabled = true,
            IsStorageEnabled = true,
        };
        _computer.Open();

        var reader = Console.In;
        var writer = Console.Out;

        string? line;
        while ((line = reader.ReadLine()) != null)
        {
            if (!line.Trim().Equals("getServerMetrics", StringComparison.OrdinalIgnoreCase))
                continue;

            var snapshot = CollectSnapshot(_computer);
            writer.WriteLine(Serialize(snapshot));
            writer.Flush();
        }

        _computer.Close();
    }

    static ServerMetricsSnapshot CollectSnapshot(Computer computer)
    {
        computer.Accept(Visitor);

        var snapshot = new ServerMetricsSnapshot();
        var coreLoads = new SortedDictionary<int, double>();

        foreach (var hardware in computer.Hardware)
        {
            CollectHardware(hardware, snapshot, coreLoads);
            foreach (var sub in hardware.SubHardware)
                CollectHardware(sub, snapshot, coreLoads);
        }

        if (!snapshot.CpuTotalPercent.HasValue && coreLoads.Count > 0)
            snapshot.CpuTotalPercent = coreLoads.Values.Average();

        if (snapshot.CpuPowerWatts is <= 0)
            snapshot.CpuPowerWatts = null;

        foreach (var kv in coreLoads)
        {
            snapshot.Cores.Add(new CoreEntry
            {
                Cpu = kv.Key.ToString(CultureInfo.InvariantCulture),
                UsagePercent = kv.Value,
            });
        }

        return snapshot;
    }

    static void CollectHardware(IHardware hardware, ServerMetricsSnapshot snapshot, SortedDictionary<int, double> coreLoads)
    {
        if (IsGpuHardware(hardware.HardwareType))
        {
            var gpu = new GpuEntry
            {
                DeviceId = GpuDeviceId(hardware),
                Name = string.IsNullOrWhiteSpace(hardware.Name) ? "GPU" : hardware.Name,
                Vendor = GpuVendor(hardware.HardwareType),
            };
            CollectGpuSensors(hardware, gpu);
            foreach (var sub in hardware.SubHardware)
                CollectGpuSensors(sub, gpu);
            if (GpuHasData(gpu))
                snapshot.Gpus.Add(gpu);
            return;
        }

        if (hardware.HardwareType == HardwareType.Memory)
            CollectMemory(hardware, snapshot.Memory);

        foreach (var sensor in hardware.Sensors)
        {
            if (!sensor.Value.HasValue)
                continue;

            var value = sensor.Value.Value;

            if (sensor.SensorType == SensorType.Temperature)
            {
                if (!IsReportedTemperature(sensor.Name, value))
                    continue;
                snapshot.Temperatures.Add(new TemperatureEntry
                {
                    Sensor = TemperatureSensorKey(hardware, sensor),
                    Celsius = value,
                });
                continue;
            }

            if (hardware.HardwareType == HardwareType.Cpu && sensor.SensorType == SensorType.Power)
            {
                if (IsPackagePower(sensor.Name))
                    snapshot.CpuPowerWatts = (snapshot.CpuPowerWatts ?? 0) + value;
                continue;
            }

            if (hardware.HardwareType != HardwareType.Cpu || sensor.SensorType != SensorType.Load)
                continue;

            if (TryParseCoreIndex(sensor.Name, out var coreIndex))
            {
                coreLoads[coreIndex] = value;
                continue;
            }

            if (IsTotalCpuLoad(sensor.Name))
                snapshot.CpuTotalPercent = value;
        }
    }

    static bool IsGpuHardware(HardwareType type) =>
        type is HardwareType.GpuNvidia or HardwareType.GpuAmd or HardwareType.GpuIntel;

    static string GpuDeviceId(IHardware hardware)
    {
        if (hardware is GenericGpu genericGpu)
        {
            var deviceId = genericGpu.DeviceId;
            if (!string.IsNullOrWhiteSpace(deviceId))
                return deviceId.Trim();
        }
        return hardware.Identifier.ToString();
    }

    static string GpuVendor(HardwareType type) => type switch
    {
        HardwareType.GpuNvidia => "nvidia",
        HardwareType.GpuAmd => "amd",
        HardwareType.GpuIntel => "intel",
        _ => "unknown",
    };

    static bool GpuHasData(GpuEntry gpu) =>
        gpu.UsagePercent.HasValue ||
        gpu.MemoryUsedBytes.HasValue ||
        gpu.MemoryTotalBytes.HasValue ||
        gpu.TemperatureCelsius.HasValue ||
        (gpu.PowerWatts.HasValue && gpu.PowerWatts > 0);

    static void CollectGpuSensors(IHardware hardware, GpuEntry gpu)
    {
        foreach (var sensor in hardware.Sensors)
        {
            if (!sensor.Value.HasValue)
                continue;

            var value = sensor.Value.Value;
            var name = sensor.Name;

            switch (sensor.SensorType)
            {
                case SensorType.Load:
                    ApplyGpuEncoderDecoderLoad(gpu, name, value);
                    ApplyGpuUsageLoad(gpu, name, value);
                    break;
                case SensorType.Temperature:
                    if (IsGpuTemperature(name, value))
                        gpu.TemperatureCelsius = PickGpuTemperature(gpu.TemperatureCelsius, name, value);
                    break;
                case SensorType.Power:
                    ApplyGpuPower(gpu, name, value);
                    break;
                case SensorType.Data:
                case SensorType.SmallData:
                    ApplyGpuMemorySensor(gpu, name, sensor, value);
                    break;
            }
        }
    }

    static void ApplyGpuEncoderDecoderLoad(GpuEntry gpu, string name, float value)
    {
        if (name.Contains("Decode", StringComparison.OrdinalIgnoreCase))
        {
            gpu.DecoderUsagePercent = Math.Max(gpu.DecoderUsagePercent, value);
            return;
        }
        if (name.Contains("Encode", StringComparison.OrdinalIgnoreCase))
            gpu.EncoderUsagePercent = Math.Max(gpu.EncoderUsagePercent, value);
    }

    static void ApplyGpuUsageLoad(GpuEntry gpu, string name, float value)
    {
        var priority = GpuUsagePriority(name);
        if (priority < 0)
            return;

        if (priority > gpu.UsagePriority)
        {
            gpu.UsagePriority = priority;
            gpu.UsagePercent = value;
            return;
        }

        if (priority == gpu.UsagePriority && gpu.UsagePercent is double current && value > current)
            gpu.UsagePercent = value;
    }

    // Match Linux nvidia-smi utilization.gpu: one overall busy %, not 3D-only.
    // Prefer "GPU Core" (NVAPI/ADL). Without it, use the max of main D3D workload engines.
    static int GpuUsagePriority(string name)
    {
        if (name.Contains("Memory", StringComparison.OrdinalIgnoreCase))
            return -1;
        if (name.Contains("Bus", StringComparison.OrdinalIgnoreCase))
            return -1;
        if (name.Equals("GPU Core", StringComparison.OrdinalIgnoreCase))
            return 100;
        if (name.Equals("GPU", StringComparison.OrdinalIgnoreCase))
            return 20;
        if (name.Contains("D3D", StringComparison.OrdinalIgnoreCase) && IsD3DWorkloadEngine(name))
            return 50;
        return -1;
    }

    static bool IsD3DWorkloadEngine(string name)
    {
        if (name.Contains("D3D 3D", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Compute", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Encode", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Decode", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Video Processing", StringComparison.OrdinalIgnoreCase))
            return true;
        return false;
    }

    static double PickGpuTemperature(double? current, string name, double value)
    {
        if (!current.HasValue)
            return value;
        if (name.Contains("Hot Spot", StringComparison.OrdinalIgnoreCase))
            return value;
        if (name.Contains("GPU Core", StringComparison.OrdinalIgnoreCase) &&
            !name.Contains("Hot Spot", StringComparison.OrdinalIgnoreCase))
            return value;
        return Math.Max(current.Value, value);
    }

    static bool IsGpuTemperature(string name, float celsius) =>
        IsReportedTemperature(name, celsius) &&
        (name.Contains("GPU", StringComparison.OrdinalIgnoreCase) ||
         name.Contains("Hot Spot", StringComparison.OrdinalIgnoreCase) ||
         name.Contains("Graphics", StringComparison.OrdinalIgnoreCase));

    static void ApplyGpuPower(GpuEntry gpu, string name, float value)
    {
        if (value <= 0)
            return;
        if (name.Contains("CPU", StringComparison.OrdinalIgnoreCase))
            return;
        if (name.Contains("Pin ", StringComparison.OrdinalIgnoreCase))
            return;

        var priority = GpuPowerPriority(name);
        if (priority > gpu.PowerPriority)
        {
            gpu.PowerPriority = priority;
            gpu.PowerWatts = value;
        }
    }

    static int GpuPowerPriority(string name)
    {
        if (name.Contains("Package", StringComparison.OrdinalIgnoreCase))
            return 100;
        if (name.Contains("Board", StringComparison.OrdinalIgnoreCase))
            return 90;
        if (name.Contains("PPT", StringComparison.OrdinalIgnoreCase))
            return 85;
        if (name.Contains("Connector", StringComparison.OrdinalIgnoreCase))
            return 80;
        if (name.Contains("Power", StringComparison.OrdinalIgnoreCase))
            return 70;
        if (name.Contains("Socket", StringComparison.OrdinalIgnoreCase))
            return 60;
        if (name.Contains("SoC", StringComparison.OrdinalIgnoreCase))
            return 50;
        if (name.Contains("Core", StringComparison.OrdinalIgnoreCase))
            return 40;
        return 30;
    }

    static void ApplyGpuMemorySensor(GpuEntry gpu, string name, ISensor sensor, float value)
    {
        var bytes = MemoryBytes(sensor, value);
        if (bytes == null)
            return;

        var usedPriority = GpuMemoryUsedPriority(name);
        if (usedPriority > gpu.MemoryUsedPriority)
        {
            gpu.MemoryUsedPriority = usedPriority;
            gpu.MemoryUsedBytes = bytes;
            return;
        }

        var totalPriority = GpuMemoryTotalPriority(name);
        if (totalPriority > gpu.MemoryTotalPriority)
        {
            gpu.MemoryTotalPriority = totalPriority;
            gpu.MemoryTotalBytes = bytes;
        }
    }

    // AMD (and NVIDIA) expose D3D dedicated VRAM separately from the Windows shared GPU
    // memory pool (often half of system RAM, e.g. 32 GB on a 64 GB machine). Prefer VRAM.
    static int GpuMemoryUsedPriority(string name)
    {
        if (name.Contains("Shared", StringComparison.OrdinalIgnoreCase))
            return -1;
        if (name.Contains("D3D Dedicated", StringComparison.OrdinalIgnoreCase) &&
            name.Contains("Used", StringComparison.OrdinalIgnoreCase))
            return 100;
        if (name.Equals("GPU Memory Used", StringComparison.OrdinalIgnoreCase))
            return 90;
        if (name.Contains("Used", StringComparison.OrdinalIgnoreCase) &&
            !name.Contains("Total", StringComparison.OrdinalIgnoreCase))
            return 50;
        return -1;
    }

    static int GpuMemoryTotalPriority(string name)
    {
        if (name.Contains("Shared", StringComparison.OrdinalIgnoreCase))
            return -1;
        if (name.Contains("D3D Dedicated", StringComparison.OrdinalIgnoreCase) &&
            name.Contains("Total", StringComparison.OrdinalIgnoreCase))
            return 100;
        if (name.Equals("GPU Memory Total", StringComparison.OrdinalIgnoreCase))
            return 90;
        if (name.Contains("Total", StringComparison.OrdinalIgnoreCase))
            return 50;
        if (name.Equals("GPU Memory", StringComparison.OrdinalIgnoreCase))
            return 40;
        return -1;
    }

    static void CollectMemory(IHardware hardware, MemoryEntry memory)
    {
        foreach (var sensor in hardware.Sensors)
        {
            if (!sensor.Value.HasValue)
                continue;

            var name = sensor.Name;
            var bytes = MemoryBytes(sensor, sensor.Value.Value);
            if (bytes == null)
                continue;

            if (name.Contains("Used", StringComparison.OrdinalIgnoreCase) &&
                !name.Contains("Swap", StringComparison.OrdinalIgnoreCase))
                memory.Used = bytes;
            else if (name.Contains("Available", StringComparison.OrdinalIgnoreCase))
                memory.Available = bytes;
            else if (name.Equals("Memory", StringComparison.OrdinalIgnoreCase) ||
                     name.Contains("Total", StringComparison.OrdinalIgnoreCase))
                memory.Total = bytes;
        }

        if (memory.Total == null && memory.Used != null && memory.Available != null)
            memory.Total = memory.Used.Value + memory.Available.Value;
    }

    static long? MemoryBytes(ISensor sensor, float value)
    {
        return sensor.SensorType switch
        {
            SensorType.Data => (long)(value * 1024L * 1024L * 1024L),
            SensorType.SmallData => (long)(value * 1024L * 1024L),
            _ => null,
        };
    }

    static bool TryParseCoreIndex(string name, out int index)
    {
        index = -1;
        foreach (var prefix in new[] { "CPU Core #", "Core #" })
        {
            if (!name.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                continue;
            var rest = name.Substring(prefix.Length);
            var space = rest.IndexOf(' ');
            if (space > 0)
                rest = rest.Substring(0, space);
            if (int.TryParse(rest, NumberStyles.Integer, CultureInfo.InvariantCulture, out index))
                return true;
        }
        return false;
    }

    static bool IsTotalCpuLoad(string name)
    {
        if (name.Equals("CPU Total", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Total CPU", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Equals("CPU", StringComparison.OrdinalIgnoreCase))
            return true;
        return false;
    }

    static bool IsPackagePower(string name)
    {
        if (name.Contains("GPU", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Contains("Core", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Contains("DRAM", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Contains("GT ", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Contains("Graphics", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Contains("SOC", StringComparison.OrdinalIgnoreCase) &&
            !name.Contains("Package", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Equals("Package", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Package", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Equals("CPU Power", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("PPT", StringComparison.OrdinalIgnoreCase))
            return true;
        if (name.Contains("Socket Power", StringComparison.OrdinalIgnoreCase))
            return true;
        return false;
    }

    static bool IsReportedTemperature(string name, float celsius)
    {
        if (celsius <= 0)
            return false;
        if (name.Contains("Distance", StringComparison.OrdinalIgnoreCase))
            return false;
        // LHM exposes NVMe/RAM threshold limits as Temperature sensors (see LHM PR #2124).
        if (name.Contains("Warning Temperature", StringComparison.OrdinalIgnoreCase))
            return false;
        if (name.Contains("Critical Temperature", StringComparison.OrdinalIgnoreCase))
            return false;
        return true;
    }

    static string TemperatureSensorKey(IHardware hardware, ISensor sensor)
    {
        var hw = hardware.Name;
        if (string.IsNullOrWhiteSpace(hw))
        {
            hw = hardware.Identifier.ToString().Replace("/", "_").TrimStart('_');
        }
        return hw + "/" + sensor.Name;
    }

    static string Serialize(ServerMetricsSnapshot s)
    {
        var sb = new StringBuilder(512);
        sb.Append('{');

        sb.Append("\"cpuTotalPercent\":");
        AppendNullableDouble(sb, s.CpuTotalPercent);

        sb.Append(",\"cpuPowerWatts\":");
        AppendNullableDouble(sb, s.CpuPowerWatts);

        sb.Append(",\"cores\":[");
        for (var i = 0; i < s.Cores.Count; i++)
        {
            if (i > 0) sb.Append(',');
            var c = s.Cores[i];
            sb.Append("{\"cpu\":");
            AppendString(sb, c.Cpu);
            sb.Append(",\"usagePercent\":");
            AppendDouble(sb, c.UsagePercent);
            sb.Append('}');
        }
        sb.Append(']');

        sb.Append(",\"memory\":{");
        sb.Append("\"used\":");
        AppendNullableLong(sb, s.Memory.Used);
        sb.Append(",\"available\":");
        AppendNullableLong(sb, s.Memory.Available);
        sb.Append(",\"total\":");
        AppendNullableLong(sb, s.Memory.Total);
        sb.Append('}');

        sb.Append(",\"temperatures\":[");
        for (var i = 0; i < s.Temperatures.Count; i++)
        {
            if (i > 0) sb.Append(',');
            var t = s.Temperatures[i];
            sb.Append("{\"sensor\":");
            AppendString(sb, t.Sensor);
            sb.Append(",\"celsius\":");
            AppendDouble(sb, t.Celsius);
            sb.Append('}');
        }
        sb.Append(']');

        sb.Append(",\"gpus\":[");
        for (var i = 0; i < s.Gpus.Count; i++)
        {
            if (i > 0) sb.Append(',');
            var g = s.Gpus[i];
            sb.Append("{\"name\":");
            AppendString(sb, g.Name);
            sb.Append(",\"vendor\":");
            AppendString(sb, g.Vendor);
            sb.Append(",\"deviceId\":");
            AppendString(sb, g.DeviceId);
            sb.Append(",\"usagePercent\":");
            AppendNullableDouble(sb, g.UsagePercent);
            sb.Append(",\"encoderUsagePercent\":");
            AppendDouble(sb, g.EncoderUsagePercent);
            sb.Append(",\"decoderUsagePercent\":");
            AppendDouble(sb, g.DecoderUsagePercent);
            sb.Append(",\"memoryUsedBytes\":");
            AppendNullableLong(sb, g.MemoryUsedBytes);
            sb.Append(",\"memoryTotalBytes\":");
            AppendNullableLong(sb, g.MemoryTotalBytes);
            sb.Append(",\"temperatureCelsius\":");
            AppendNullableDouble(sb, g.TemperatureCelsius);
            sb.Append(",\"powerWatts\":");
            AppendNullableDouble(sb, g.PowerWatts);
            sb.Append('}');
        }
        sb.Append("]}");

        return sb.ToString();
    }

    static void AppendString(StringBuilder sb, string value)
    {
        sb.Append('"');
        foreach (var ch in value)
        {
            switch (ch)
            {
                case '\\': sb.Append("\\\\"); break;
                case '"': sb.Append("\\\""); break;
                case '\n': sb.Append("\\n"); break;
                case '\r': sb.Append("\\r"); break;
                case '\t': sb.Append("\\t"); break;
                default:
                    if (ch < 0x20)
                        sb.AppendFormat(CultureInfo.InvariantCulture, "\\u{0:X4}", (int)ch);
                    else
                        sb.Append(ch);
                    break;
            }
        }
        sb.Append('"');
    }

    static void AppendDouble(StringBuilder sb, double value) =>
        sb.Append(value.ToString("0.########", CultureInfo.InvariantCulture));

    static void AppendNullableDouble(StringBuilder sb, double? value) =>
        sb.Append(value.HasValue ? value.Value.ToString("0.########", CultureInfo.InvariantCulture) : "null");

    static void AppendNullableLong(StringBuilder sb, long? value) =>
        sb.Append(value.HasValue ? value.Value.ToString(CultureInfo.InvariantCulture) : "null");
}
