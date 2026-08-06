#!/usr/bin/env sh
set -eu

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 48 | tr -d '\n'
  else
    python3 - <<'PY'
import base64, secrets
print(base64.b64encode(secrets.token_bytes(48)).decode(), end='')
PY
  fi
}

cat <<ENV
POSTGRES_PASSWORD=$(random_secret)
RABBITMQ_PASSWORD=$(random_secret)
APP_JWT_SECRET=$(random_secret)
APP_ENCRYPTION_KEY=$(random_secret)
APP_HASH_KEY=$(random_secret)
APP_BOOTSTRAP_ENABLED=false
APP_PAYMENT_EXECUTION_ENABLED=true
APP_BANK_SANDBOX_FALLBACK_ENABLED=true
APP_BANK_ALLOW_UNCERTIFIED_PROVIDERS=true
ENV
