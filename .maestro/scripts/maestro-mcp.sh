#!/bin/bash
# Starts the Maestro MCP server with the pinned Maestro and Java from toolchain.properties.
# MCP uses stdout, so the toolchain setup output goes to stderr.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."
source .maestro/scripts/configure-toolchain.sh >&2
exec maestro mcp "$@"
