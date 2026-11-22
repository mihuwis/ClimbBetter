param([int]$ApiPort = 18081, [int]$Port = 5175)
$ErrorActionPreference = 'Stop'
$modelEnvironment = @{
    VITE_AUTH_MODE = 'emulator'
    VITE_FIREBASE_PROJECT_ID = 'demo-climbbetter'
    VITE_FIREBASE_AUTH_DOMAIN = 'localhost'
    VITE_FIREBASE_API_KEY = 'local-emulator'
    VITE_FIREBASE_APP_ID = 'local-climbbetter'
    CB_API_PROXY_TARGET = "http://127.0.0.1:$ApiPort"
}
$originalModelEnvironment = @{}
foreach ($variableName in $modelEnvironment.Keys) {
    $originalModelEnvironment[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
    [Environment]::SetEnvironmentVariable($variableName, $modelEnvironment[$variableName], 'Process')
}
Push-Location (Join-Path $PSScriptRoot '../frontend-web')
try {
    & npm.cmd run dev -- --host 127.0.0.1 --port $Port --strictPort
} finally {
    Pop-Location
    foreach ($variableName in $originalModelEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($variableName, $originalModelEnvironment[$variableName], 'Process')
    }
}
