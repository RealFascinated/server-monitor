# Builds the embedded LibreHardwareMonitor helper for Windows agent binaries.
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$project = Join-Path $root "internal\lhm\sensorhost\get_sensors.csproj"

dotnet build -c Release $project
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "LHM helper built at internal\lhm\sensorhost\bin\Release\net48"
Write-Host "Build the agent with: go build -tags lhmbundle -o monitor-agent.exe ./cmd/main.go"
