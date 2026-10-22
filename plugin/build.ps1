$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$javaBin = 'C:/Users/Yuma/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/bin'
$libs = @(Get-ChildItem 'E:/Minecraft/server/libraries' -Recurse -Filter '*.jar' | ForEach-Object FullName)
$classpath = $libs -join ';'
$classes = Join-Path $root 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = @(Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$javaBin/javac.exe" -encoding UTF-8 -source 25 -target 25 -classpath $classpath -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$javaBin/java.exe" -cp $classes cn.tarot.CoreTest
if ($LASTEXITCODE -ne 0) { throw 'Rules test failed' }
Copy-Item (Join-Path $root 'resources/*') $classes -Force
$output = [IO.Path]::GetFullPath((Join-Path $root '../../outputs/TarotRainbow-3.0.0.jar'))
$tempDir = Join-Path $root 'tmp'
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
& "$javaBin/jar.exe" "-J-Djava.io.tmpdir=$tempDir" --create --file $output -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed' }
Write-Output "Built $output"
