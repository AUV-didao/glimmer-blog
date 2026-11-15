<#
  重置 SQLite 数据库到迁移后的种子状态（会覆盖现有数据，仅用于演示/测试）。
  用法：pwsh -File scripts/reset-db.ps1 -Force
#>
param(
    [string]$Database = (Join-Path $PSScriptRoot '..\data\blog.db'),
    [string]$Seed = (Join-Path $PSScriptRoot '..\sql\blog-seed.db'),
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
if (-not $Force) {
    Write-Host '该脚本会用种子数据库覆盖所有演示数据。' -ForegroundColor Yellow
    Write-Host '确认请追加 -Force 重新执行。' -ForegroundColor Yellow
    exit 1
}
$Database = [System.IO.Path]::GetFullPath($Database)
$Seed = [System.IO.Path]::GetFullPath($Seed)
$reset = Join-Path $PSScriptRoot 'reset-sqlite.py'
& python $reset --database $Database --seed $Seed
if ($LASTEXITCODE -ne 0) { throw "SQLite 复位失败，退出码 $LASTEXITCODE" }
Write-Host "完成：$Database" -ForegroundColor Green
Write-Host '演示账号：admin/123456、lin/123456' -ForegroundColor Green
