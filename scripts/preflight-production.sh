#!/usr/bin/env sh
set -eu

required='SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD SPRING_RABBITMQ_HOST SPRING_RABBITMQ_USERNAME SPRING_RABBITMQ_PASSWORD APP_JWT_SECRET APP_ENCRYPTION_KEY APP_HASH_KEY APP_OUTBOUND_ALLOWED_HOSTS'
for name in $required; do
  eval "value=\${$name-}"
  [ -n "$value" ] || { echo "Missing required environment variable: $name" >&2; exit 1; }
done

[ "${SPRING_PROFILES_ACTIVE:-}" = 'prod' ] || { echo 'SPRING_PROFILES_ACTIVE must be prod.' >&2; exit 1; }
[ "${APP_BOOTSTRAP_ENABLED:-false}" = 'false' ] || { echo 'APP_BOOTSTRAP_ENABLED must be false.' >&2; exit 1; }
[ "${APP_BANK_ALLOW_UNCERTIFIED_PROVIDERS:-false}" = 'false' ] || { echo 'Uncertified bank providers must be disabled.' >&2; exit 1; }
[ "${APP_BANK_SANDBOX_FALLBACK_ENABLED:-false}" = 'false' ] || { echo 'Sandbox fallback must be disabled.' >&2; exit 1; }

if [ "${APP_PAYMENT_EXECUTION_ENABLED:-false}" = 'true' ]; then
  [ -n "${APP_BANK_CERTIFIED_PROVIDERS:-}" ] || { echo 'Go-live requires APP_BANK_CERTIFIED_PROVIDERS.' >&2; exit 1; }
  case ",${APP_BANK_CERTIFIED_PROVIDERS}," in
    *',REST_GENERIC,'*|*',SANDBOX,'*) echo 'REST_GENERIC and SANDBOX cannot be certified providers.' >&2; exit 1 ;;
  esac
fi

echo 'Production environment preflight passed. No secret values were printed.'
