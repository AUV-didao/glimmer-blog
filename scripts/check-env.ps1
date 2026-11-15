<#
  环境自检：Java、SQLite 数据文件、应用端口与首页。
  用法：pwsh -File scripts/check-env.ps1
#>
param(
    [string]$BaseUrl = 'http://127.0.0.1:8082/blog',
    [string]$Database = (Join-Path $PSScriptRoot '..\data\blog.db')
)

$ErrorActionPreference = 'Stop'
$fail = 0
function Ok($name, $detail) { Write-Host ("[ OK ] {0}  {1}" -f $name, $detail) -ForegroundColor Green }
function Bad($name, $detail) { Write-Host ("[FAIL] {0}  {1}" -f $name, $detail) -ForegroundColor Red; $script:fail++ }

try {
    $v = (& java -version 2>&1 | Select-Object -First 1)
    Ok 'Java' $v
} catch { Bad 'Java' 'java 不在 PATH 中' }

$root = Split-Path $PSScriptRoot -Parent
foreach ($p in @('src\main\java', 'src\main\webapp', 'sql\blog-sqlite.sql', 'pom.xml')) {
    if (Test-Path (Join-Path $root $p)) { Ok "工程文件 $p" '' } else { Bad "工程文件 $p" '缺失' }
}

$Database = [System.IO.Path]::GetFullPath($Database)
if (-not (Test-Path $Database -PathType Leaf)) {
    Bad 'SQLite 数据库' "文件不存在：$Database"
} else {
    $query = Join-Path $PSScriptRoot 'sqlite-query.py'
    $tables = & python $query $Database "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';" 2>$null
    if ($LASTEXITCODE -eq 0 -and [int]($tables -join '') -eq 8) { Ok 'SQLite 数据库' "8 张表；$Database" } else { Bad 'SQLite 数据库' '无法读取或表数量不是 8' }
    $integrity = & python $query $Database 'PRAGMA integrity_check;' 2>$null
    if ($LASTEXITCODE -eq 0 -and ($integrity -join '') -eq 'ok') { Ok 'SQLite 完整性' 'integrity_check=ok' } else { Bad 'SQLite 完整性' ($integrity -join '; ') }
}

try {
    $r = Invoke-WebRequest -Uri $BaseUrl -UseBasicParsing -TimeoutSec 8
    Ok '应用' "$BaseUrl -> $($r.StatusCode)"
} catch { Bad '应用' "$BaseUrl 无法访问（启动 Tomcat，context=/blog，端口 8082）" }

Write-Host ''
if ($fail -eq 0) { Write-Host '环境检查全部通过。' -ForegroundColor Green } else { Write-Host "$fail 项需要处理。" -ForegroundColor Yellow }
exit $fail
