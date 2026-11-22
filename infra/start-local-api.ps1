param(
    [ValidateSet('firebase', 'emulator', 'stub')][string]$AuthMode = 'firebase',
    [string]$ProjectId = 'climbbetter-dev',
    [string]$CredentialsFile = '',
    [int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
$originalAuthEnvironment = @{}
foreach ($variableName in @('CB_AUTH_MODE','CB_FIREBASE_PROJECT_ID','GOOGLE_APPLICATION_CREDENTIALS','FIREBASE_AUTH_EMULATOR_HOST','CB_DB_URL','CB_LOCAL_USER_ID')) {
    $originalAuthEnvironment[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
}
try {
$env:CB_AUTH_MODE = $AuthMode
if ($AuthMode -eq 'firebase') {
    if ($env:FIREBASE_AUTH_EMULATOR_HOST) { throw 'Remove FIREBASE_AUTH_EMULATOR_HOST before using real Firebase.' }
    if ($CredentialsFile) { $env:GOOGLE_APPLICATION_CREDENTIALS = (Resolve-Path -LiteralPath $CredentialsFile).Path }
    if (-not $env:GOOGLE_APPLICATION_CREDENTIALS) { throw 'Provide -CredentialsFile with the local service-account JSON path.' }
    if (-not (Test-Path -LiteralPath $env:GOOGLE_APPLICATION_CREDENTIALS -PathType Leaf)) { throw 'The credentials file does not exist.' }
    $credentialMetadata = Get-Content -Raw -LiteralPath $env:GOOGLE_APPLICATION_CREDENTIALS | ConvertFrom-Json
    if ($credentialMetadata.project_id -ne $ProjectId) { throw 'The credentials belong to a different Firebase project.' }
    $env:CB_FIREBASE_PROJECT_ID = $ProjectId
    $env:CB_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/climbbetter'
} elseif ($AuthMode -eq 'emulator') {
    $env:CB_FIREBASE_PROJECT_ID = 'demo-climbbetter'
    $env:FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099'
    $env:CB_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/climbbetter_models'
} else {
    $env:CB_LOCAL_USER_ID = '00000000-0000-0000-0000-000000000002'
    $env:CB_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/climbbetter'
}
Push-Location (Join-Path $PSScriptRoot '../backend')
try {
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local' "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=$Port --debug=false --logging.level.root=INFO --logging.level.org.springframework=INFO"
    if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
}
} finally {
    foreach ($variableName in $originalAuthEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($variableName, $originalAuthEnvironment[$variableName], 'Process')
    }
}
