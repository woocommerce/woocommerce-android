#!/bin/bash -eu
#
# Run Maestro smoke tests locally with credentials loaded from .maestro/.env
#
# Usage:
#   ./scripts/run-maestro-local.sh .maestro/flows/login_successful.yaml
#   ./scripts/run-maestro-local.sh .maestro/flows/
#   ./scripts/run-maestro-local.sh --include-tags=orders .maestro/flows/
#
# Credentials are loaded from .maestro/.env (copy from .maestro/env.example).
# You can also export MAESTRO_WOO_* vars in your shell instead.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.maestro/.env"

# Load .env file if present
if [[ -f "$ENV_FILE" ]]; then
  echo "Loading credentials from $ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "No .maestro/.env found."
  echo "Either export MAESTRO_WOO_* vars in your shell, or:"
  echo "  cp .maestro/env.example .maestro/.env"
  echo "  # Edit .maestro/.env with your credentials"
  echo ""
fi

# Validate required vars
MISSING_VARS=()
for var in MAESTRO_WOO_EMAIL MAESTRO_WOO_PASSWORD MAESTRO_WOO_STORE_URL; do
  if [[ -z "${!var:-}" ]]; then
    MISSING_VARS+=("$var")
  fi
done

if [[ ${#MISSING_VARS[@]} -gt 0 ]]; then
  echo "ERROR: Missing required variables: ${MISSING_VARS[*]}"
  echo "See .maestro/env.example for the full list of variables."
  exit 1
fi

echo "Using store: $MAESTRO_WOO_STORE_URL"
echo "Using email: $MAESTRO_WOO_EMAIL"
echo ""

exec maestro test "$@"
