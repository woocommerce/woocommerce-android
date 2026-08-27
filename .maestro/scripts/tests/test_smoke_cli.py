#!/usr/bin/env python3
"""Public CLI contract tests for the Android Maestro smoke runner."""

from __future__ import annotations

import os
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent.parent
RUNNER = REPO_ROOT / ".maestro" / "scripts" / "run-smoke-tests.sh"
DOCTOR = REPO_ROOT / ".maestro" / "scripts" / "doctor.py"
GOLDEN_DIR = SCRIPT_DIR / "golden"


class SmokeCliContractTest(unittest.TestCase):
    def test_device_media_fixture_is_prepared_only_for_the_media_flow(self) -> None:
        source = RUNNER.read_text(encoding="utf-8")

        self.assertIn('"$(basename "$flow")" == "products_media_upload.yaml"', source)
        self.assertIn('if [[ ! -f "$MEDIA_FIXTURE" ]]; then', source)
        self.assertIn('adb -s "$DEVICE_SERIAL" push "$MEDIA_FIXTURE"', source)
        self.assertLess(
            source.index("prepare_device_media_fixture\n"),
            source.index("validate_google_login_apk()"),
        )

    def test_cleanup_finishes_before_reports_and_participates_in_exit_status(self) -> None:
        source = RUNNER.read_text(encoding="utf-8")

        self.assertLess(
            source.index('echo "--- Cleaning run-owned fixtures"'),
            source.index('echo "--- Generating reports"'),
        )
        self.assertIn('|| "$CLEANUP_FAILED" -gt 0', source)
        self.assertNotIn('cleanup --manifest "$MANIFEST_FILE" --store "$STORE" || true', source)

    def test_destructive_flows_are_not_blindly_retried(self) -> None:
        source = RUNNER.read_text(encoding="utf-8")

        self.assertIn('if flow_has_any_tag "$flow" destructive; then', source)
        self.assertIn('echo "  destructive flow failed; automatic retry is disabled"', source)

    def run_runner(self, *args: str) -> tuple[subprocess.CompletedProcess[str], Path]:
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        output_root = Path(temporary_directory.name) / "output"
        env = {
            **os.environ,
            "PATH": "/usr/bin:/bin",
            "WOO_MAESTRO_ENV_FILE": str(Path(temporary_directory.name) / "missing.env"),
            "WOO_MAESTRO_OUTPUT_DIR": str(output_root),
        }
        result = subprocess.run(
            [str(RUNNER), *args],
            cwd=REPO_ROOT,
            env=env,
            capture_output=True,
            text=True,
            check=False,
        )
        return result, output_root

    def run_with_fake_device_tools(
        self,
        *args: str,
        env_overrides: dict[str, str] | None = None,
        maestro_version: str = "2.8.0",
    ) -> tuple[subprocess.CompletedProcess[str], Path]:
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        temporary_path = Path(temporary_directory.name)
        fake_bin = temporary_path / "bin"
        fake_bin.mkdir()
        adb_marker = temporary_path / "adb-invoked"

        maestro = fake_bin / "maestro"
        maestro.write_text(
            "#!/bin/sh\n"
            "if [ \"${1:-}\" = --version ]; then\n"
            f"  printf '%s\\n' '{maestro_version}'\n"
            "fi\n"
        )
        maestro.chmod(0o755)
        java = fake_bin / "java"
        java.write_text("#!/bin/sh\nprintf '%s\\n' 'openjdk version \"21.0.8\"' >&2\n")
        java.chmod(0o755)
        adb = fake_bin / "adb"
        adb.write_text(
            "#!/bin/sh\n"
            f": > '{adb_marker}'\n"
            "printf 'List of devices attached\\n'\n"
        )
        adb.chmod(0o755)

        env = {key: value for key, value in os.environ.items() if not key.startswith("MAESTRO_WOO_")}
        env.update(
            {
                "CI": "1",
                "HOME": str(temporary_path),
                "PATH": f"{fake_bin}:/usr/bin:/bin",
                "WOO_MAESTRO_ENV_FILE": str(temporary_path / "missing.env"),
                "WOO_MAESTRO_OUTPUT_DIR": str(temporary_path / "output"),
            }
        )
        env.update(env_overrides or {})
        result = subprocess.run(
            [str(RUNNER), *args],
            cwd=REPO_ROOT,
            env=env,
            capture_output=True,
            text=True,
            check=False,
        )
        return result, adb_marker

    def run_with_order_recording_tools(
        self,
        *args: str,
        env_overrides: dict[str, str],
    ) -> tuple[subprocess.CompletedProcess[str], list[str]]:
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        temporary_path = Path(temporary_directory.name)
        fake_bin = temporary_path / "bin"
        fake_bin.mkdir()
        events = temporary_path / "events"

        maestro = fake_bin / "maestro"
        maestro.write_text(
            "#!/bin/sh\n"
            "if [ \"${1:-}\" = --version ]; then printf '%s\\n' '2.8.0'; fi\n"
        )
        maestro.chmod(0o755)
        java = fake_bin / "java"
        java.write_text("#!/bin/sh\nprintf '%s\\n' 'openjdk version \"21.0.8\"' >&2\n")
        java.chmod(0o755)
        adb = fake_bin / "adb"
        adb.write_text(
            "#!/bin/sh\n"
            f"printf 'adb\\n' >> '{events}'\n"
            "printf 'List of devices attached\\n'\n"
        )
        adb.chmod(0o755)
        seed_script = temporary_path / "seed-fixtures"
        seed_script.write_text(
            "#!/bin/sh\n"
            "command=$1\n"
            "shift\n"
            f"printf '%s\\n' \"$command\" >> '{events}'\n"
            "manifest=\n"
            "while [ $# -gt 0 ]; do\n"
            "  if [ \"$1\" = --manifest ]; then manifest=$2; shift 2; else shift; fi\n"
            "done\n"
            "if [ \"$command\" = lock ] && [ -n \"$manifest\" ]; then\n"
            "  printf '{\"lock\": {\"id\": 1}, \"entities\": []}\\n' > \"$manifest\"\n"
            "fi\n"
        )
        seed_script.chmod(0o755)

        env = {key: value for key, value in os.environ.items() if not key.startswith("MAESTRO_WOO_")}
        env.update(
            {
                "CI": "1",
                "HOME": str(temporary_path),
                "PATH": f"{fake_bin}:/usr/bin:/bin",
                "WOO_MAESTRO_ENV_FILE": str(temporary_path / "missing.env"),
                "WOO_MAESTRO_OUTPUT_DIR": str(temporary_path / "output"),
                "WOO_MAESTRO_SEED_SCRIPT": str(seed_script),
                **env_overrides,
            }
        )
        result = subprocess.run(
            [str(RUNNER), *args],
            cwd=REPO_ROOT,
            env=env,
            capture_output=True,
            text=True,
            check=False,
        )
        recorded_events = events.read_text().splitlines() if events.exists() else []
        return result, recorded_events

    def run_with_recorded_maestro_args(
        self,
        *args: str,
        env_overrides: dict[str, str],
    ) -> tuple[subprocess.CompletedProcess[str], str]:
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        temporary_path = Path(temporary_directory.name)
        fake_bin = temporary_path / "bin"
        fake_bin.mkdir()
        maestro_args = temporary_path / "maestro-args"

        maestro = fake_bin / "maestro"
        maestro.write_text(
            "#!/bin/sh\n"
            "if [ \"${1:-}\" = --version ]; then printf '%s\\n' '2.8.0'; exit 0; fi\n"
            f"printf 'ARGS:%s\\n' \"$*\" >> '{maestro_args}'\n"
            f"env | grep -E '^(MAESTRO_)?WOO_' | sort >> '{maestro_args}'\n"
        )
        maestro.chmod(0o755)
        java = fake_bin / "java"
        java.write_text("#!/bin/sh\nprintf '%s\\n' 'openjdk version \"21.0.8\"' >&2\n")
        java.chmod(0o755)
        adb = fake_bin / "adb"
        adb.write_text(
            "#!/bin/sh\n"
            "if [ \"${1:-}\" = devices ]; then\n"
            "  printf 'List of devices attached\\nemulator-5554\\tdevice\\n'\n"
            "elif printf '%s\\n' \"$*\" | grep -q 'settings get global'; then\n"
            "  printf '1\\n'\n"
            "fi\n"
        )
        adb.chmod(0o755)

        env = {key: value for key, value in os.environ.items() if not key.startswith("MAESTRO_WOO_")}
        env.update(
            {
                "CI": "1",
                "HOME": str(temporary_path),
                "PATH": f"{fake_bin}:/usr/bin:/bin",
                "WOO_MAESTRO_ENV_FILE": str(temporary_path / "missing.env"),
                "WOO_MAESTRO_OUTPUT_DIR": str(temporary_path / "output"),
                **env_overrides,
            }
        )
        result = subprocess.run(
            [str(RUNNER), *args],
            cwd=REPO_ROOT,
            env=env,
            capture_output=True,
            text=True,
            check=False,
        )
        return result, maestro_args.read_text(encoding="utf-8") if maestro_args.exists() else ""

    def run_core_with_recorded_maestro_args(self) -> tuple[subprocess.CompletedProcess[str], str]:
        return self.run_with_recorded_maestro_args(
            "--profile",
            "core",
            "--device",
            "emulator-5554",
            env_overrides={
                "MAESTRO_WOO_LAB_JETPACK_STORE_URL": "https://lab.example.com/",
                "MAESTRO_WOO_LAB_WPCOM_EMAIL": "lab@example.com",
                "MAESTRO_WOO_LAB_WPCOM_PASSWORD": "selected-password",
                "MAESTRO_WOO_LAB_CONSUMER_SECRET": "selected-rest-secret",
                "MAESTRO_WOO_SHARED_WPCOM_PASSWORD": "other-store-secret",
            },
        )

    def assert_golden(self, result: subprocess.CompletedProcess[str], name: str) -> None:
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, "")
        self.assertEqual(result.stdout, (GOLDEN_DIR / name).read_text())

    def test_core_plan_is_side_effect_free(self) -> None:
        result, output_root = self.run_runner("--plan", "--profile", "core")

        self.assert_golden(result, "core-plan.txt")
        self.assertFalse(output_root.exists())

    def test_phone_full_plan_includes_quarantined_phone_flows(self) -> None:
        result, _ = self.run_runner("--plan", "--profile", "phone-full")

        self.assert_golden(result, "phone-full-plan.txt")

    def test_release_plan_excludes_quarantine(self) -> None:
        result, _ = self.run_runner("--plan", "--profile", "release")

        self.assert_golden(result, "release-plan.txt")

    def test_burst_plan_repeats_release_selection(self) -> None:
        result, _ = self.run_runner("--plan", "--profile", "burst")

        self.assert_golden(result, "burst-plan.txt")

    def test_extended_plan_requires_explicit_quarantine_opt_in(self) -> None:
        result, _ = self.run_runner(
            "--plan",
            "--include-tags",
            "smoke_extended",
            "--include-quarantine",
            "--store",
            "lab",
        )

        self.assert_golden(result, "smoke-extended-plan.txt")

    def test_doctor_treats_zero_selected_flows_as_fatal(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            temporary_path = Path(temporary_directory)
            fake_bin = temporary_path / "bin"
            fake_bin.mkdir()
            for name, body in {
                "maestro": "#!/bin/sh\nprintf '%s\\n' '2.8.0'\n",
                "java": "#!/bin/sh\nprintf '%s\\n' 'openjdk version \"21.0.8\"' >&2\n",
                "adb": "#!/bin/sh\nprintf 'List of devices attached\\n'\n",
            }.items():
                executable = fake_bin / name
                executable.write_text(body)
                executable.chmod(0o755)

            env = {
                **os.environ,
                "PATH": f"{fake_bin}:/usr/bin:/bin",
            }
            result = subprocess.run(
                [
                    str(DOCTOR),
                    "--profile",
                    "core",
                    "--include-tags",
                    "does_not_exist",
                    "--env-file",
                    str(temporary_path / "missing.env"),
                ],
                cwd=REPO_ROOT,
                env=env,
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(result.returncode, 1)
        self.assertIn("[FAIL] 0 flow(s) selected for profile core", result.stdout)

    def test_doctor_reports_a_toolchain_mismatch(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            temporary_path = Path(temporary_directory)
            fake_bin = temporary_path / "bin"
            fake_bin.mkdir()
            for name, body in {
                "maestro": "#!/bin/sh\nprintf '%s\\n' '2.7.0'\n",
                "java": "#!/bin/sh\nprintf '%s\\n' 'openjdk version \"21.0.8\"' >&2\n",
                "adb": "#!/bin/sh\nprintf 'List of devices attached\\n'\n",
            }.items():
                executable = fake_bin / name
                executable.write_text(body)
                executable.chmod(0o755)

            env = {**os.environ, "PATH": f"{fake_bin}:/usr/bin:/bin"}
            result = subprocess.run(
                [
                    str(DOCTOR),
                    "--profile",
                    "core",
                    "--env-file",
                    str(temporary_path / "missing.env"),
                ],
                cwd=REPO_ROOT,
                env=env,
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(result.returncode, 1)
        self.assertIn("[FAIL] Maestro version mismatch: expected 2.8.0, actual 2.7.0", result.stdout)

    def test_plan_treats_zero_selected_flows_as_fatal(self) -> None:
        result, output_root = self.run_runner(
            "--plan",
            "--include-tags",
            "smoke_extended",
            "--store",
            "lab",
        )

        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, "")
        self.assertEqual(result.stderr, "No flows matched the current filters.\n")
        self.assertFalse(output_root.exists())

    def test_shared_destructive_run_requires_seed_before_adb(self) -> None:
        result, adb_marker = self.run_with_fake_device_tools(
            "--store",
            "shared",
            ".maestro/flows/orders_create.yaml",
            env_overrides={
                "MAESTRO_WOO_SHARED_JETPACK_STORE_URL": "https://inpersonpayments.wpcomstaging.com/",
                "MAESTRO_WOO_SHARED_WPCOM_EMAIL": "shared@example.com",
                "MAESTRO_WOO_SHARED_WPCOM_PASSWORD": "shared-password",
                "MAESTRO_WOO_SHARED_CONSUMER_KEY": "ck_shared",
                "MAESTRO_WOO_SHARED_CONSUMER_SECRET": "cs_shared",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertIn("Shared destructive runs require --seed", result.stderr)
        self.assertFalse(adb_marker.exists())

    def test_runtime_rejects_a_mismatched_maestro_before_adb(self) -> None:
        result, adb_marker = self.run_with_fake_device_tools(
            "--store",
            "lab",
            ".maestro/flows/login_successful.yaml",
            maestro_version="2.7.0",
            env_overrides={
                "MAESTRO_WOO_LAB_JETPACK_STORE_URL": "https://lab.example.com/",
                "MAESTRO_WOO_LAB_WPCOM_EMAIL": "lab@example.com",
                "MAESTRO_WOO_LAB_WPCOM_PASSWORD": "lab-password",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertIn("Maestro version mismatch: expected 2.8.0, actual 2.7.0", result.stderr)
        self.assertFalse(adb_marker.exists())

    def test_lab_and_generic_credentials_cannot_satisfy_shared_destructive_preflight(self) -> None:
        result, adb_marker = self.run_with_fake_device_tools(
            "--store",
            "shared",
            "--seed",
            ".maestro/flows/orders_create.yaml",
            env_overrides={
                "MAESTRO_WOO_LAB_JETPACK_STORE_URL": "https://lab.example.com/",
                "MAESTRO_WOO_LAB_WPCOM_EMAIL": "lab@example.com",
                "MAESTRO_WOO_LAB_WPCOM_PASSWORD": "lab-password",
                "MAESTRO_WOO_LAB_CONSUMER_KEY": "ck_lab",
                "MAESTRO_WOO_LAB_CONSUMER_SECRET": "cs_lab",
                "MAESTRO_WOO_JETPACK_STORE_URL": "https://lab.example.com/",
                "MAESTRO_WOO_WPCOM_EMAIL": "lab@example.com",
                "MAESTRO_WOO_WPCOM_PASSWORD": "lab-password",
                "MAESTRO_WOO_CONSUMER_KEY": "ck_lab",
                "MAESTRO_WOO_CONSUMER_SECRET": "cs_lab",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertIn("Missing scoped shared store configuration", result.stderr)
        self.assertFalse(adb_marker.exists())

    def test_shared_destructive_preflight_requires_the_exact_shared_host(self) -> None:
        result, adb_marker = self.run_with_fake_device_tools(
            "--store",
            "shared",
            "--seed",
            ".maestro/flows/orders_create.yaml",
            env_overrides={
                "MAESTRO_WOO_SHARED_JETPACK_STORE_URL": "https://lookalike.example.com/",
                "MAESTRO_WOO_SHARED_WPCOM_EMAIL": "shared@example.com",
                "MAESTRO_WOO_SHARED_WPCOM_PASSWORD": "shared-password",
                "MAESTRO_WOO_SHARED_CONSUMER_KEY": "ck_shared",
                "MAESTRO_WOO_SHARED_CONSUMER_SECRET": "cs_shared",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertIn(
            "Shared destructive runs require host inpersonpayments.wpcomstaging.com",
            result.stderr,
        )
        self.assertFalse(adb_marker.exists())

    def test_shared_destructive_lock_is_acquired_before_adb(self) -> None:
        result, events = self.run_with_order_recording_tools(
            "--store",
            "shared",
            "--seed",
            ".maestro/flows/orders_create.yaml",
            env_overrides={
                "MAESTRO_WOO_SHARED_JETPACK_STORE_URL": "https://inpersonpayments.wpcomstaging.com/",
                "MAESTRO_WOO_SHARED_WPCOM_EMAIL": "shared@example.com",
                "MAESTRO_WOO_SHARED_WPCOM_PASSWORD": "shared-password",
                "MAESTRO_WOO_SHARED_CONSUMER_KEY": "ck_shared",
                "MAESTRO_WOO_SHARED_CONSUMER_SECRET": "cs_shared",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertEqual(events, ["lock", "adb", "unlock"])

    def test_seed_request_does_not_create_unused_fixtures_without_destructive_flows(self) -> None:
        result, events = self.run_with_order_recording_tools(
            "--profile",
            "release",
            "--seed",
            env_overrides={
                "MAESTRO_WOO_SHARED_JETPACK_STORE_URL": "https://inpersonpayments.wpcomstaging.com/",
                "MAESTRO_WOO_SHARED_WPCOM_EMAIL": "shared@example.com",
                "MAESTRO_WOO_SHARED_WPCOM_PASSWORD": "shared-password",
                "MAESTRO_WOO_SHARED_CONSUMER_KEY": "ck_shared",
                "MAESTRO_WOO_SHARED_CONSUMER_SECRET": "cs_shared",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertEqual(events, ["adb"])
        self.assertIn("No destructive flows selected; skipping fixture seeding", result.stdout)

    def test_generic_credentials_cannot_satisfy_a_scoped_lab_selection(self) -> None:
        result, adb_marker = self.run_with_fake_device_tools(
            "--store",
            "lab",
            ".maestro/flows/login_successful.yaml",
            env_overrides={
                "MAESTRO_WOO_JETPACK_STORE_URL": "https://lab.example.com/",
                "MAESTRO_WOO_WPCOM_EMAIL": "lab@example.com",
                "MAESTRO_WOO_WPCOM_PASSWORD": "lab-password",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertIn("Missing required env var: MAESTRO_WOO_JETPACK_STORE_URL", result.stderr)
        self.assertFalse(adb_marker.exists())

    def test_maestro_cli_receives_only_selected_flow_values_and_no_rest_or_other_store_secrets(self) -> None:
        result, args = self.run_core_with_recorded_maestro_args()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("MAESTRO_WOO_WPCOM_PASSWORD=selected-password", args)
        self.assertNotIn("\nWOO_WPCOM_PASSWORD=selected-password\n", args)
        for line in args.splitlines():
            if line.startswith("ARGS:"):
                self.assertNotIn("selected-password", line)
        self.assertNotIn("selected-rest-secret", args)
        self.assertNotIn("other-store-secret", args)

    def test_wordpress_dot_com_not_woo_store_requires_explicit_wpcom_credentials(self) -> None:
        result, args = self.run_with_recorded_maestro_args(
            "--device",
            "emulator-5554",
            ".maestro/flows/login_not_woo_store.yaml",
            env_overrides={
                "MAESTRO_WOO_NOT_A_WOO_STORE_URL": "https://not-woo.wordpress.com/",
                "MAESTRO_WOO_NOT_A_WOO_STORE_SITE_ADMIN_USERNAME": "site-admin",
                "MAESTRO_WOO_NOT_A_WOO_STORE_SITE_ADMIN_PASSWORD": "site-password",
            },
        )

        self.assertEqual(result.returncode, 1)
        self.assertIn("WordPress.com-hosted not-Woo-store fixtures require both", result.stderr)
        self.assertEqual("", args)

    def test_not_woo_store_forwards_explicit_wpcom_credentials(self) -> None:
        result, args = self.run_with_recorded_maestro_args(
            "--device",
            "emulator-5554",
            ".maestro/flows/login_not_woo_store.yaml",
            env_overrides={
                "MAESTRO_WOO_NOT_A_WOO_STORE_URL": "https://not-woo.wordpress.com/",
                "MAESTRO_WOO_NOT_A_WOO_STORE_SITE_ADMIN_USERNAME": "site-admin",
                "MAESTRO_WOO_NOT_A_WOO_STORE_SITE_ADMIN_PASSWORD": "site-password",
                "MAESTRO_WOO_NOT_A_WOO_STORE_WPCOM_EMAIL": "wpcom-user",
                "MAESTRO_WOO_NOT_A_WOO_STORE_WPCOM_PASSWORD": "wpcom-password",
            },
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("MAESTRO_WOO_NOT_A_WOO_STORE_WPCOM_EMAIL=wpcom-user", args)
        self.assertIn("MAESTRO_WOO_NOT_A_WOO_STORE_WPCOM_PASSWORD=wpcom-password", args)

    def test_no_jetpack_wp_admin_url_is_normalized_before_maestro(self) -> None:
        result, args = self.run_with_recorded_maestro_args(
            "--device",
            "emulator-5554",
            ".maestro/flows/login_no_jetpack.yaml",
            env_overrides={
                "MAESTRO_WOO_NO_JETPACK_SITE_URL": "https://shop.example.com/subdir/wp-admin/?source=jn",
                "MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_USERNAME": "site-admin",
                "MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_PASSWORD": "site-password",
            },
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("MAESTRO_WOO_NO_JETPACK_SITE_URL=https://shop.example.com/subdir/", args)
        self.assertNotIn("/wp-admin/", args)
        self.assertNotIn("source=jn", args)


if __name__ == "__main__":
    unittest.main()
