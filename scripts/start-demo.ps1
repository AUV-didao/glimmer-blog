<# Run from Windows PowerShell 5.1+ or PowerShell 7. Requires JDK and Tomcat 9. #>
param(
    [Parameter(Mandatory=$true)][string]$TomcatHome,
    [ValidateRange(1024,65535)][int]$Port = 8082
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$TomcatHome = (Resolve-Path $TomcatHome).Path
if (-not (Test-Path (Join-Path $TomcatHome 'bin\catalina.bat'))) { throw 'Tomcat bin/catalina.bat not found.' }
$war = Join-Path $root 'dist\blog.war'
if (-not (Test-Path $war)) { $war = Join-Path $root 'target\blog.war' }
if (-not (Test-Path $war)) { throw 'Build with mvn clean package first, or use the complete ZIP containing dist/blog.war.' }
$data = Join-Path $root 'data'
New-Item -ItemType Directory -Path $data -Force | Out-Null
$db = Join-Path $data 'blog.db'
if (-not (Test-Path $db)) { Copy-Item (Join-Path $root 'sql\blog-seed.db') $db }
$base = Join-Path $root '.runtime\tomcat'
if (Test-Path (Join-Path $base 'webapps\blog.war')) {
    throw 'An existing demo deployment was found. Stop it first; preserve its upload folder before removing .runtime/tomcat. This avoids overwriting uploaded files.'
}
foreach ($dir in @('conf','logs','temp','webapps','work')) { New-Item -ItemType Directory (Join-Path $base $dir) -Force | Out-Null }
Copy-Item (Join-Path $TomcatHome 'conf\*') (Join-Path $base 'conf') -Recurse -Force
[xml]$server = Get-Content (Join-Path $base 'conf\server.xml') -Raw
$server.Server.SetAttribute('port', '-1')
$service = $server.Server.Service
foreach ($connector in @($service.Connector)) {
    if ($connector.GetAttribute('protocol') -like '*AJP*') { [void]$service.RemoveChild($connector) }
    else { $connector.SetAttribute('port',[string]$Port); $connector.SetAttribute('address','127.0.0.1') }
}
$server.Save((Join-Path $base 'conf\server.xml'))
Copy-Item $war (Join-Path $base 'webapps\blog.war')
$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $base
$env:BLOG_DB_PATH = $db
Write-Host "Open http://127.0.0.1:$Port/blog/ after startup. Demo only: admin/123456, lin/123456."
& (Join-Path $TomcatHome 'bin\catalina.bat') run
