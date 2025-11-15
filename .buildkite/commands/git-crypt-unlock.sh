#!/bin/bash 

set -euo pipefail

echo "$GIT_CRYPT_ENCRYPTION_KEY" | base64 -d | git-crypt unlock -
