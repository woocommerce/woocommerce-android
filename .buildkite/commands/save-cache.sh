#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

# .buildkite/commands/prototype-build.sh -> build_and_upload_prototype_build
# -> prototype_flavor = 'Jalapeno'
# -> prototype_build_type = 'Debug'
echo "--- 🛠 Download Mobile App Dependencies [Assemble Mobile App]"
./gradlew :WooCommerce:assembleJalapenoDebug
echo ""

# .buildkite/commands/prototype-build.sh -> build_and_upload_prototype_build
# -> prototype_flavor = 'Jalapeno'
# -> prototype_build_type = 'Debug'
echo "--- 🛠 Download Wear App Dependencies [Assemble Wear App]"
./gradlew :WooCommerce-Wear:assembleJalapenoDebug
echo ""

# .buildkite/commands/lint.sh -> ./gradlew :WooCommerce:lintJalapenoDebug
echo "--- 🧹 Download Lint Dependencies [Lint Mobile App]"
./gradlew :WooCommerce:lintJalapenoDebug
echo ""

# .buildkite/commands/run-unit-tests.sh -> ./gradlew testJalapenoDebugUnitTest testDebugUnitTest
echo "--- 🧪 Download Unit Test Dependencies [Assemble Unit Tests]"
./gradlew assembleJalapenoDebugUnitTest assembleDebugUnitTest
echo ""

# .buildkite/commands/run-instrumented-tests.sh -> build_and_instrumented_test
#  -> gradle(tasks: %w[assembleVanillaDebug assembleVanillaDebugAndroidTest])
echo "--- 🧪 Download Android Test Dependencies [Assemble Android Tests]"
./gradlew assembleJalapenoDebugAndroidTest
echo ""

echo "--- 💾 Save Cache"
save_gradle_dependency_cache
