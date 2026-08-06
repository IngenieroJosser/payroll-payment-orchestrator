$ErrorActionPreference = 'Stop'
$required = @(
    'SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD',
    'SPRING_RABBITMQ_HOST','SPRING_RABBITMQ_USERNAME','SPRING_RABBITMQ_PASSWORD',
    'APP_JWT_SECRET','APP_ENCRYPTION_KEY','APP_HASH_KEY','APP_OUTBOUND_ALLOWED_HOSTS'
)
foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value)) { throw "Missing required environment variable: $name" }
}
if ($env:SPRING_PROFILES_ACTIVE -ne 'prod') { throw 'SPRING_PROFILES_ACTIVE must be prod.' }
if (($env:APP_BOOTSTRAP_ENABLED ?? 'false') -ne 'false') { throw 'APP_BOOTSTRAP_ENABLED must be false.' }
if (($env:APP_BANK_ALLOW_UNCERTIFIED_PROVIDERS ?? 'false') -ne 'false') { throw 'Uncertified providers must be disabled.' }
if (($env:APP_BANK_SANDBOX_FALLBACK_ENABLED ?? 'false') -ne 'false') { throw 'Sandbox fallback must be disabled.' }
if (($env:APP_PAYMENT_EXECUTION_ENABLED ?? 'false') -eq 'true') {
    if ([string]::IsNullOrWhiteSpace($env:APP_BANK_CERTIFIED_PROVIDERS)) { throw 'Go-live requires APP_BANK_CERTIFIED_PROVIDERS.' }
    $providers = $env:APP_BANK_CERTIFIED_PROVIDERS.Split(',') | ForEach-Object { $_.Trim().ToUpperInvariant() }
    if ($providers -contains 'REST_GENERIC' -or $providers -contains 'SANDBOX') {
        throw 'REST_GENERIC and SANDBOX cannot be certified providers.'
    }
}
Write-Host 'Production environment preflight passed. No secret values were printed.'
