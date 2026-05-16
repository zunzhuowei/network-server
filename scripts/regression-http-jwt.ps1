# HTTP + JWT 端到端回归（需已启动 regression 集群）
param(
    [string]$GatewayHost = "127.0.0.1",
    [int]$GatewayPort = 25555,
    [int]$TimeoutSec = 15
)

$ErrorActionPreference = "Stop"
$base = "http://${GatewayHost}:${GatewayPort}"
$passed = 0
$failed = 0

function Assert-Condition {
    param([string]$Name, [bool]$Ok, [string]$Detail = "")
    if ($Ok) {
        Write-Host "[PASS] $Name" -ForegroundColor Green
        $script:passed++
    } else {
        Write-Host "[FAIL] $Name $Detail" -ForegroundColor Red
        $script:failed++
    }
}

function Invoke-HttpGet {
    param([string]$Url, [hashtable]$Headers = @{})
    $params = @{
        Uri             = $Url
        Method          = "GET"
        TimeoutSec      = $TimeoutSec
        UseBasicParsing = $true
    }
    if ($Headers.Count -gt 0) {
        $params["Headers"] = $Headers
    }
    try {
        $resp = Invoke-WebRequest @params
        return @{ Status = $resp.StatusCode; Body = $resp.Content }
    } catch {
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $body = $reader.ReadToEnd()
            return @{ Status = [int]$_.Exception.Response.StatusCode; Body = $body }
        }
        throw
    }
}

Write-Host "=== HTTP + JWT E2E ($base) ===" -ForegroundColor Cyan

# 1. 公开接口 /index
$r1 = Invoke-HttpGet -Url "$base/index"
Assert-Condition "GET /index returns 200" ($r1.Status -eq 200)
Assert-Condition "GET /index body contains zun" ($r1.Body -match "zun")

# 2. 受保护接口无 Token（成功响应为 JSON status=ok，拒绝则为 HTML 或其它内容）
$r2 = Invoke-HttpGet -Url "$base/regression/protected"
$denied = $r2.Body -notmatch '"status"\s*:\s*"ok"'
Assert-Condition "GET /regression/protected without token denied" $denied "status=$($r2.Status) body=$($r2.Body)"

# 3. 登录获取 JWT
$r3 = Invoke-HttpGet -Url "$base/regression/login"
Assert-Condition "GET /regression/login returns 200" ($r3.Status -eq 200)
$loginJson = $r3.Body | ConvertFrom-Json
$token = $loginJson.token
Assert-Condition "login returns token" (-not [string]::IsNullOrWhiteSpace($token))

# 4. 带 Authentication 访问受保护接口（项目使用 Authentication 头，非 Authorization）
$r4 = Invoke-HttpGet -Url "$base/regression/protected" -Headers @{ Authentication = $token }
$okJson = $r4.Body | ConvertFrom-Json
Assert-Condition "GET /regression/protected with token returns 200" ($r4.Status -eq 200)
Assert-Condition "protected body status ok" ($okJson.status -eq "ok")

# 5. 篡改 Token
$r5 = Invoke-HttpGet -Url "$base/regression/protected" -Headers @{ Authentication = "$token.tampered" }
$denied5 = $r5.Body -notmatch '"status"\s*:\s*"ok"'
Assert-Condition "GET /regression/protected with bad token denied" $denied5 "body=$($r5.Body)"

Write-Host ""
Write-Host "Result: passed=$passed failed=$failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
if ($failed -gt 0) { exit 1 }
