from __future__ import annotations

import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]


class MaestroCiContractTests(unittest.TestCase):
    def test_given_login_credentials_when_referenced_then_they_keep_the_maestro_prefix(self) -> None:
        login_files = [
            *sorted((REPO_ROOT / ".maestro" / "flows").glob("login_*.yaml")),
            REPO_ROOT / ".maestro" / "subflows" / "login.yaml",
            REPO_ROOT / ".maestro" / "subflows" / "ensure_configured_woo_store.yaml",
        ]

        referenced_credentials = []
        for path in login_files:
            text = path.read_text(encoding="utf-8")
            with self.subTest(path=path):
                self.assertNotIn("${WOO_", text)
                self.assertNotIn("String(WOO_", text)
            referenced_credentials.extend(
                token for token in text.split() if "MAESTRO_WOO_" in token
            )

        self.assertTrue(referenced_credentials)

        runner = (
            REPO_ROOT / ".maestro" / "scripts" / "run-smoke-tests.sh"
        ).read_text(encoding="utf-8")
        self.assertIn('if [[ ${#FLOW_ENV_REFS[@]} -gt 0 ]]; then', runner)

    def test_given_login_flows_when_opening_site_address_then_they_share_qr_aware_entry(self) -> None:
        helper_path = REPO_ROOT / ".maestro" / "subflows" / "open_site_address_login.yaml"
        helper = helper_path.read_text(encoding="utf-8")
        strings = ET.parse(
            REPO_ROOT / "WooCommerce" / "src" / "main" / "res" / "values" / "strings.xml"
        ).getroot()
        fallback = strings.find("string[@name='login_qr_prologue_fallback_link']")

        self.assertIsNotNone(fallback)
        self.assertIsNotNone(fallback.text)
        self.assertIn("? ", fallback.text)
        self.assertIn(fallback.text.split("? ", maxsplit=1)[1], helper)
        self.assertIn('id: "input"', helper)
        self.assertLess(
            helper.index('visible: "^Log In$|'),
            helper.index('id: "button_login_store"'),
        )

        direct_login_flows = [
            "login_google.yaml",
            "login_help.yaml",
            "login_no_jetpack.yaml",
            "login_not_woo_store.yaml",
            "login_not_wp_site.yaml",
            "login_wrong_account.yaml",
            "login_wrong_credentials.yaml",
        ]
        for flow_name in direct_login_flows:
            with self.subTest(flow=flow_name):
                flow = (REPO_ROOT / ".maestro" / "flows" / flow_name).read_text(encoding="utf-8")
                self.assertIn("../subflows/open_site_address_login.yaml", flow)

        reusable_login = (
            REPO_ROOT / ".maestro" / "subflows" / "login.yaml"
        ).read_text(encoding="utf-8")
        self.assertIn("file: open_site_address_login.yaml", reusable_login)

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
