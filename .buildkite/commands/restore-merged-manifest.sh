#!/bin/bash -e

MODULE=$1
BUILD_VARIANT=$2

echo "--- 💾 Restore Debug Manifest (Jalapeno)"
restore_android_merged_manifest ${MODULE} ${BUILD_VARIANT} || true
