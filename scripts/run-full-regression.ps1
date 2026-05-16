# 全方位回归：编译、单元/集成测试、集群启动、HTTP+JWT、TCP 端口、清理
param(
    [switch]$SkipBuild,
    [switch]$KeepCluster
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$clusterPids = @()

function Stop-Cluster {
    foreach ($proc in $clusterPids) {
        if ($proc -and -not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Get-Process java -ErrorAction SilentlyContinue | Where-Object {
        $_.Path -like "*network-server*" -or $_.CommandLine -like "*room-server*" -or $_.CommandLine -like "*hall-server*" -or $_.CommandLine -like "*gateway-server*"
    } | Stop-Process -Force -ErrorAction SilentlyContinue
}

function Wait-Port {
    param([int]$Port, [int]$Retries = 30)
    for ($i = 0; $i -lt $Retries; $i++) {
        $r = Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -WarningAction SilentlyContinue
        if ($r.TcpTestSucceeded) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Start-Node {
    param([string]$Jar, [string]$Name)
    $logOut = Join-Path (Split-Path $Jar) "$Name-regression.log"
    $logErr = Join-Path (Split-Path $Jar) "$Name-regression.err"
    $p = Start-Process java -ArgumentList "-jar", $Jar, "--spring.profiles.active=regression" -PassThru `
        -RedirectStandardOutput $logOut -RedirectStandardError $logErr -WindowStyle Hidden
    $script:clusterPids += $p
    return $p
}

try {
    Write-Host "=== [1/5] Maven clean test ===" -ForegroundColor Cyan
    if (-not $SkipBuild) {
        mvn clean test
        if ($LASTEXITCODE -ne 0) { throw "mvn test failed" }
    }

    Write-Host "=== [2/5] Package servers ===" -ForegroundColor Cyan
    mvn package -DskipTests -pl gateway-server,hall-server,room-server -am -q
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed" }

    Write-Host "=== [3/5] Start regression cluster ===" -ForegroundColor Cyan
    $roomJar = Join-Path $root "room-server\target\room-server-1.0.0.jar"
    $hallJar = Join-Path $root "hall-server\target\hall-server-1.0.0.jar"
    $gwJar   = Join-Path $root "gateway-server\target\gateway-server-1.0.0.jar"

    Start-Node -Jar $roomJar -Name "room" | Out-Null
    Start-Sleep -Seconds 4
    Start-Node -Jar $hallJar -Name "hall" | Out-Null
    Start-Sleep -Seconds 4
    $gwProc = Start-Process java -ArgumentList "-jar", $gwJar, "--spring.profiles.active=regression", "--spring.profiles.include=" -PassThru `
        -RedirectStandardOutput (Join-Path $root "gateway-server\target\gw-regression.log") `
        -RedirectStandardError (Join-Path $root "gateway-server\target\gw-regression.err") -WindowStyle Hidden
    $clusterPids += $gwProc
    Start-Sleep -Seconds 8

    foreach ($port in @(26006, 26003, 26000, 25555)) {
        if (-not (Wait-Port -Port $port)) {
            throw "Port $port not listening"
        }
        Write-Host "[OK] Port $port listening" -ForegroundColor Green
    }

    Write-Host "=== [4/5] HTTP + JWT E2E script ===" -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "regression-http-jwt.ps1")
    if ($LASTEXITCODE -ne 0) { throw "HTTP JWT script failed" }

    Write-Host "=== [5/5] TCP smoke (gateway inside port) ===" -ForegroundColor Cyan
    $tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 26000 -WarningAction SilentlyContinue
    if (-not $tcp.TcpTestSucceeded) { throw "TCP 26000 not reachable" }
    Write-Host "[OK] TCP 26000 reachable" -ForegroundColor Green

    Write-Host ""
    Write-Host "=== FULL REGRESSION PASSED ===" -ForegroundColor Green
}
finally {
    if (-not $KeepCluster) {
        Write-Host "Stopping cluster..." -ForegroundColor Yellow
        Stop-Cluster
    } else {
        Write-Host "Cluster left running (KeepCluster)." -ForegroundColor Yellow
    }
}
