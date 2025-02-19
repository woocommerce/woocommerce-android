#!/bin/bash -e

MODULE=$1

echo "--- 💾 Restore Debug Manifest (Jalapeno)"
restore_android_merged_manifest ${MODULE} || true
