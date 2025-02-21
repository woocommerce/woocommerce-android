#!/bin/bash -e

MODULE=$1
BUILD_VARIANT=$2

echo "--- 💾 Restore Merged Manifest (Module: ${MODULE}, Build Variant: ${BUILD_VARIANT})"
restore_android_merged_manifest ${MODULE} ${BUILD_VARIANT} || true
