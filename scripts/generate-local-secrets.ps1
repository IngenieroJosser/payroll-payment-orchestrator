$ErrorActionPreference = 'Stop'

function New-RandomSecret([int]$Bytes = 48) {
    $buffer = New-Object byte[] $Bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($buffer)
    [Convert]::ToBase64String($buffer)
}

@"
POSTGRES_PASSWORD=$(New-RandomSecret)
RABBITMQ_PASSWORD=$(New-RandomSecret)
APP_JWT_SECRET=$(New-RandomSecret)
APP_ENCRYPTION_KEY=$(New-RandomSecret)
APP_HASH_KEY=$(New-RandomSecret)
APP_BOOTSTRAP_ENABLED=false
APP_PAYMENT_EXECUTION_ENABLED=true
APP_BANK_SANDBOX_FALLBACK_ENABLED=true
APP_BANK_ALLOW_UNCERTIFIED_PROVIDERS=true
"@
