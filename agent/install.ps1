#Requires -Version 5.1
<#
.SYNOPSIS
  Install or remove the Monitor agent on Windows.

.EXAMPLE
  Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.ps1 -UseBasicParsing).Content)) install YOUR_INGEST_TOKEN

.EXAMPLE
  .\install.ps1 install YOUR_INGEST_TOKEN

.EXAMPLE
  .\install.ps1 install YOUR_INGEST_TOKEN -BinaryPath .\monitor-agent.exe

.EXAMPLE
  .\install.ps1 uninstall
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('install', 'uninstall')]
    [string] $Command,

    [Parameter(Position = 1)]
    [string] $IngestToken,

    [string] $Version,
    [string] $ApiEndpoint = 'https://monitor.fascinated.cc/api/v1/servers/ingest',
    [ValidateSet('true', 'false')]
    [string] $AutoUpdate = 'true',
    [string] $InstallDir = "$env:ProgramData\MonitorAgent",
    [string] $BinaryPath,
    [string] $ServiceName = 'monitor-agent',
    [switch] $Help
)

$ErrorActionPreference = 'Stop'
$GitHubRepo = 'RealFascinated/Monitor-API'
$AgentPath = 'agent'
$NssmZipUrl = 'https://nssm.cc/release/nssm-2.24.zip'

function Show-Usage {
    @"
Install or remove the Monitor agent on Windows (NSSM service).

Usage:
  .\install.ps1 install <ingest_token> [options]
  .\install.ps1 uninstall

Options (install):
  -Version VERSION       Release version (default: latest GitHub agent/v* tag)
  -ApiEndpoint URL       Ingest API endpoint
  -AutoUpdate true|false  Daily self-update task (default: true)
  -InstallDir PATH       Install directory (default: $env:ProgramData\MonitorAgent)
  -BinaryPath PATH       Use a local binary instead of downloading a release
  -ServiceName NAME      Windows service name (default: monitor-agent)

Examples:
  Set-ExecutionPolicy Bypass -Scope Process -Force; & ([ScriptBlock]::Create((iwr https://raw.githubusercontent.com/RealFascinated/Monitor-API/master/agent/install.ps1 -UseBasicParsing).Content)) install YOUR_INGEST_TOKEN
  .\install.ps1 install YOUR_INGEST_TOKEN
  .\install.ps1 install YOUR_INGEST_TOKEN -BinaryPath .\monitor-agent.exe
  .\install.ps1 uninstall
"@
}

function Write-Log([string]$Message) {
    Write-Host "==> $Message"
}

function Invoke-Quiet([scriptblock]$Block) {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $Block 2>&1 | Out-Null
    } finally {
        $ErrorActionPreference = $prev
    }
}

function Test-ServiceRegistered([string]$Name) {
    $output = (sc.exe query $Name 2>&1 | Out-String).Trim()
    if ($output -match '(?i)1060|does not exist') { return $false }
    return $output -match '(?i)SERVICE_NAME'
}

function Wait-ServiceRemoved([string]$Name, [int]$TimeoutSeconds = 90) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-ServiceRegistered -Name $Name)) { return }
        Start-Sleep -Seconds 1
    }
    throw @"
Timed out waiting for service '$Name' to finish deleting.
Close Services (services.msc) if it is open, stop any monitor-agent.exe processes, then retry.
"@
}

function Stop-MonitorAgentService([string]$Nssm) {
    if (-not (Test-ServiceRegistered -Name $ServiceName)) { return }

    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -ne 'Stopped') {
        Write-Log "Stopping service $ServiceName"
        if ($Nssm -and (Test-Path $Nssm)) {
            Invoke-Quiet { & $Nssm stop $ServiceName confirm }
        }
        Invoke-Quiet { sc.exe stop $ServiceName }
    }

    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        if (-not (Get-Process -Name 'monitor-agent' -ErrorAction SilentlyContinue)) { return }
        Start-Sleep -Seconds 1
    }

    $procs = Get-Process -Name 'monitor-agent' -ErrorAction SilentlyContinue
    if ($procs) {
        Write-Log 'Stopping monitor-agent.exe processes'
        $procs | Stop-Process -Force
        Start-Sleep -Seconds 1
    }
}

function Remove-ExistingService([string]$Nssm) {
    if (-not (Test-ServiceRegistered -Name $ServiceName)) { return }

    Write-Log "Removing existing service $ServiceName"
    Stop-MonitorAgentService -Nssm $Nssm
    if ($Nssm -and (Test-Path $Nssm)) {
        Invoke-Quiet { & $Nssm remove $ServiceName confirm }
    }
    Invoke-Quiet { sc.exe delete $ServiceName }
    Wait-ServiceRemoved -Name $ServiceName
}

function Assert-Administrator {
    $principal = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'Run as Administrator (elevated PowerShell).'
    }
}

function Escape-YamlString([string]$Value) {
    return ($Value -replace '\\', '\\' -replace '"', '\"')
}

function Get-LatestVersion {
    $releases = Invoke-RestMethod -Uri "https://api.github.com/repos/$GitHubRepo/releases?per_page=100"
    $versions = foreach ($release in $releases) {
        if ($release.draft -or $release.prerelease) {
            continue
        }
        if ($release.tag_name -match '^agent/v(.+)$') {
            $Matches[1]
        }
    }
    $latest = $versions | Sort-Object { [version]$_ } -Descending | Select-Object -First 1
    if (-not $latest) {
        throw 'Could not determine latest agent release version.'
    }
    return $latest
}

function Install-AgentBinary([string]$Version, [string]$Destination) {
    if ($BinaryPath) {
        $source = Resolve-Path $BinaryPath
        Write-Log "Installing local binary from $source"
        Copy-Item -Path $source -Destination $Destination -Force
        return
    }

    $arch = 'amd64'
    $tag = "agent/v$Version"
    $asset = "monitor-agent-windows-$arch.exe"
    $baseUrl = "https://github.com/$GitHubRepo/releases/download/$tag"
    $tmp = Join-Path $env:TEMP "monitor-agent-$Version-checksums.txt"
    $staging = Join-Path $env:TEMP "monitor-agent-$Version.exe"

    Write-Log "Downloading $asset ($tag)"
    Invoke-WebRequest -Uri "$baseUrl/$asset" -OutFile $staging
    Invoke-WebRequest -Uri "$baseUrl/checksums.txt" -OutFile $tmp

    $hashLine = (Get-Content $tmp | Where-Object { $_ -match " $([regex]::Escape($asset))`$" })
    if (-not $hashLine) { throw "Checksum entry not found for $asset" }
    $expected = ($hashLine -split '\s+')[0].ToLower()
    $actual = (Get-FileHash -Path $staging -Algorithm SHA256).Hash.ToLower()
    if ($expected -ne $actual) {
        throw "Checksum mismatch for $asset"
    }
    Copy-Item -Path $staging -Destination $Destination -Force
}

function Get-ConfigTemplatePath([string]$Version, [string]$Destination) {
    $local = Join-Path $PSScriptRoot 'config-example.yml'
    if (Test-Path $local) {
        Copy-Item -Path $local -Destination $Destination -Force
        return
    }

    $tag = "agent/v$Version"
    $baseUrl = "https://github.com/$GitHubRepo/releases/download/$tag"
    Write-Log "Downloading config-example.yml ($tag)"
    try {
        Invoke-WebRequest -Uri "$baseUrl/config-example.yml" -OutFile $Destination
        return
    } catch {
        Write-Log 'config-example.yml not in release, using main branch template'
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/$GitHubRepo/master/$AgentPath/config-example.yml" -OutFile $Destination
    }
}

function Write-AgentConfig([string]$Path, [string]$TemplatePath) {
    if (-not (Test-Path $TemplatePath)) {
        throw "config template not found: $TemplatePath"
    }

    $token = Escape-YamlString $IngestToken
    $endpoint = Escape-YamlString $ApiEndpoint
    $content = Get-Content -Raw -Path $TemplatePath
    $content = $content -replace '(?m)^ingest_token:.*', "ingest_token: `"$token`""
    $content = $content -replace '(?m)^api_endpoint:.*', "api_endpoint: `"$endpoint`""
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $content, $utf8)
}

function Set-NssmParameter([string]$Nssm, [string]$Key, [string]$Value) {
    & $Nssm set $ServiceName $Key $Value | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "nssm set $ServiceName $Key failed (exit $LASTEXITCODE)"
    }
}

function Show-ServiceDiagnostics([string]$Nssm, [string]$Exe, [string]$Log) {
    Write-Host ''
    Write-Host '--- NSSM Application ---'
    & $Nssm get $ServiceName Application 2>&1
    Write-Host '--- NSSM AppDirectory ---'
    & $Nssm get $ServiceName AppDirectory 2>&1
    Write-Host '--- NSSM AppEnvironmentExtra ---'
    & $Nssm get $ServiceName AppEnvironmentExtra 2>&1
    if (Test-Path $Log) {
        Write-Host '--- agent.log (last 40 lines) ---'
        Get-Content $Log -Tail 40
    } else {
        Write-Host "--- agent.log not found at $Log ---"
    }
    Write-Host '--- manual test (same working directory as service) ---'
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    Push-Location (Split-Path $Exe)
    & $Exe print 2>&1 | Select-Object -First 5
    Pop-Location
    $ErrorActionPreference = $prev
}

function Get-NssmExe([string]$ToolsDir) {
    $nssm = Join-Path $ToolsDir 'nssm.exe'
    if (Test-Path $nssm) { return $nssm }

    Write-Log 'Downloading NSSM'
    $zipPath = Join-Path $env:TEMP 'nssm-2.24.zip'
    Invoke-WebRequest -Uri $NssmZipUrl -OutFile $zipPath
    $extract = Join-Path $env:TEMP 'nssm-extract'
    if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }
    Expand-Archive -Path $zipPath -DestinationPath $extract -Force
    $candidate = Get-ChildItem -Path $extract -Recurse -Filter 'nssm.exe' |
        Where-Object { $_.FullName -match '\\win64\\' } |
        Select-Object -First 1
    if (-not $candidate) { throw 'nssm.exe not found in NSSM archive' }
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
    Copy-Item -Path $candidate.FullName -Destination $nssm -Force
    return $nssm
}

function Install-Service([string]$Nssm, [string]$Exe, [string]$Dir, [string]$LogFile, [string]$ConfigFile) {
    Remove-ExistingService -Nssm $Nssm

    Write-Log "Installing service $ServiceName"
    & $Nssm install $ServiceName $Exe 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "nssm install $ServiceName failed (exit $LASTEXITCODE). The service may still be deleting; wait a minute and retry."
    }
    Set-NssmParameter -Nssm $Nssm -Key AppDirectory -Value $Dir
    # Absolute config path (same as install.sh systemd Environment=); do not rely on service CWD alone.
    $configEnv = "MONITOR_CONFIG_FILE=$ConfigFile"
    if ($ConfigFile -match '\s') {
        $configEnv = "MONITOR_CONFIG_FILE=`"$ConfigFile`""
    }
    Set-NssmParameter -Nssm $Nssm -Key AppEnvironmentExtra -Value $configEnv
    Set-NssmParameter -Nssm $Nssm -Key DisplayName -Value 'Monitor Agent'
    Set-NssmParameter -Nssm $Nssm -Key Description -Value 'Monitor host metrics agent'
    Invoke-Quiet { & $Nssm set $ServiceName Start SERVICE_AUTO_START }
    Set-NssmParameter -Nssm $Nssm -Key AppStdout -Value $LogFile
    Set-NssmParameter -Nssm $Nssm -Key AppStderr -Value $LogFile
    Invoke-Quiet { & $Nssm set $ServiceName AppRotateFiles 1 }
    Invoke-Quiet { & $Nssm set $ServiceName AppRotateBytes 1048576 }
    Invoke-Quiet { & $Nssm set $ServiceName AppExit Default Restart }
    Invoke-Quiet { & $Nssm set $ServiceName AppRestartDelay 5000 }
}

function Install-UpdateTask([string]$Exe) {
    $taskName = "$ServiceName-update"
    $existing = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if ($existing) {
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    }

    Write-Log "Scheduling daily self-update ($taskName)"
    $action = New-ScheduledTaskAction -Execute $Exe -Argument 'update' -WorkingDirectory (Split-Path $Exe)
    $trigger = New-ScheduledTaskTrigger -Daily -At '04:17'
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable
    $principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal | Out-Null
}

function Install-MonitorAgent {
    if (-not $IngestToken) { throw 'install requires an ingest token' }

    if (-not $Version) {
        $Version = Get-LatestVersion
    }

    New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
    $toolsDir = Join-Path $InstallDir 'tools'
    $exe = Join-Path $InstallDir 'monitor-agent.exe'
    $config = Join-Path $InstallDir 'config.yml'
    $log = Join-Path $InstallDir 'agent.log'
    $nssmPath = Join-Path $toolsDir 'nssm.exe'
    if (-not (Test-Path $nssmPath)) { $nssmPath = $null }

    Stop-MonitorAgentService -Nssm $nssmPath
    Install-AgentBinary -Version $Version -Destination $exe
    $configTemplate = Join-Path $InstallDir 'config-example.yml'
    Get-ConfigTemplatePath -Version $Version -Destination $configTemplate

    Write-Log "Writing config to $config"
    Write-AgentConfig -Path $config -TemplatePath $configTemplate

    $nssm = Get-NssmExe -ToolsDir $toolsDir
    Install-Service -Nssm $nssm -Exe $exe -Dir $InstallDir -LogFile $log -ConfigFile $config

    Write-Log 'Starting service'
    Invoke-Quiet { & $nssm start $ServiceName }
    Start-Sleep -Seconds 4

    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if (-not $svc -or $svc.Status -ne 'Running') {
        Show-ServiceDiagnostics -Nssm $nssm -Exe $exe -Log $log
        throw "Service $ServiceName did not reach Running state (status: $($svc.Status))"
    }

    if ($AutoUpdate -eq 'true') {
        Install-UpdateTask -Exe $exe
    }

    Write-Log "Monitor agent installed. Logs: $log"
    Get-Service $ServiceName
}

function Uninstall-MonitorAgent {
    $exe = Join-Path $InstallDir 'monitor-agent.exe'
    $nssm = Join-Path $InstallDir 'tools\nssm.exe'
    $taskName = "$ServiceName-update"

    if (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue) {
        Write-Log "Removing scheduled task $taskName"
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    }

    $nssmPath = if (Test-Path $nssm) { $nssm } else { $null }
    Remove-ExistingService -Nssm $nssmPath

    if (Test-Path $InstallDir) {
        Write-Log "Removing $InstallDir"
        Remove-Item -Recurse -Force $InstallDir
    }

    Write-Log 'Uninstall complete'
}

if ($Help) {
    Show-Usage
    exit 0
}

if (-not $Command) {
    Show-Usage
    exit 1
}

Assert-Administrator

switch ($Command) {
    'install' { Install-MonitorAgent }
    'uninstall' { Uninstall-MonitorAgent }
    default { Show-Usage; exit 1 }
}
