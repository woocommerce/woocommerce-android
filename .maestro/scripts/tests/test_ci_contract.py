from __future__ import annotations

import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]


class MaestroCiContractTests(unittest.TestCase):
    def test_toolchain_is_configured_before_building_the_app(self) -> None:
        wrapper = (
            REPO_ROOT / ".buildkite" / "commands" / "run-maestro-tests.sh"
        ).read_text(encoding="utf-8")

        self.assertLess(
            wrapper.index("source .maestro/scripts/configure-toolchain.sh"),
            wrapper.index("./gradlew :WooCommerce:installWasabiDebug"),
        )

    def test_changed_file_skip_only_applies_to_pull_requests(self) -> None:
        wrapper = (
            REPO_ROOT / ".buildkite" / "commands" / "run-maestro-tests.sh"
        ).read_text(encoding="utf-8")

        self.assertIn(
            'if [[ "${BUILDKITE_PULL_REQUEST:-false}" != "false" ]] &&',
            wrapper,
        )
        self.assertIn(
            ".buildkite/commands/should-skip-job.sh --job-type validation; then",
            wrapper,
        )

    def test_shared_store_steps_are_serialized(self) -> None:
        pipeline_files = [
            REPO_ROOT / ".buildkite" / "pipeline.yml",
            REPO_ROOT / ".buildkite" / "schedules" / "maestro-smoke-burst.yml",
            REPO_ROOT / ".buildkite" / "release-pipelines" / "maestro-smoke.yml",
        ]

        for path in pipeline_files:
            with self.subTest(path=path):
                text = path.read_text(encoding="utf-8")
                self.assertIn('concurrency_group: "woocommerce-android/maestro/shared-store"', text)
                self.assertIn("concurrency: 1", text)

    def test_shared_destructive_ci_runs_seed_owned_fixtures(self) -> None:
        pipeline_files = [
            REPO_ROOT / ".buildkite" / "schedules" / "maestro-smoke-burst.yml",
            REPO_ROOT / ".buildkite" / "release-pipelines" / "maestro-smoke.yml",
        ]

        for path in pipeline_files:
            with self.subTest(path=path):
                text = path.read_text(encoding="utf-8")
                self.assertIn('MAESTRO_SEED: "true"', text)


if __name__ == "__main__":
    unittest.main()
