from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


SCRIPT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPT_DIR))

import ensure_release_app as release_app  # noqa: E402


class EnsureReleaseAppTests(unittest.TestCase):
    def test_latest_stable_release_selects_the_verified_universal_apk(self) -> None:
        asset = release_app.parse_release_asset(
            {
                "tag_name": "25.4",
                "draft": False,
                "prerelease": False,
                "assets": [
                    {
                        "name": "WooCommerce-25.4.aab",
                        "browser_download_url": "https://github.com/example/app.aab",
                        "size": 12,
                        "digest": "sha256:" + "1" * 64,
                    },
                    {
                        "name": "WooCommerce-25.4-universal.apk",
                        "browser_download_url": "https://github.com/example/app.apk",
                        "size": 42,
                        "digest": "sha256:" + "a" * 64,
                    },
                ],
            }
        )

        self.assertEqual(asset.tag, "25.4")
        self.assertEqual(asset.name, "WooCommerce-25.4-universal.apk")
        self.assertEqual(asset.sha256, "a" * 64)

    def test_release_asset_without_a_sha256_digest_is_rejected(self) -> None:
        with self.assertRaisesRegex(release_app.ReleaseAppError, "SHA-256"):
            release_app.parse_release_asset(
                {
                    "tag_name": "25.4",
                    "draft": False,
                    "prerelease": False,
                    "assets": [
                        {
                            "name": "WooCommerce-25.4-universal.apk",
                            "browser_download_url": "https://github.com/example/app.apk",
                            "size": 42,
                            "digest": None,
                        }
                    ],
                }
            )

    def test_production_apk_badging_must_not_be_debuggable(self) -> None:
        metadata = release_app.parse_apk_badging(
            "package: name='com.woocommerce.android' versionCode='778' versionName='25.4'\n"
        )

        self.assertEqual(metadata.package_name, "com.woocommerce.android")
        self.assertEqual(metadata.version_name, "25.4")
        self.assertFalse(metadata.debuggable)

        debuggable = release_app.parse_apk_badging(
            "package: name='com.woocommerce.android' versionCode='778' versionName='25.4'\n"
            "application-debuggable\n"
        )
        self.assertTrue(debuggable.debuggable)

    def test_dev_apk_is_rejected(self) -> None:
        badging = release_app.subprocess.CompletedProcess(
            args=[],
            returncode=0,
            stdout=(
                "package: name='com.woocommerce.android.dev' "
                "versionCode='778' versionName='25.4-rc-1'\n"
            ),
            stderr="",
        )

        with tempfile.TemporaryDirectory() as temporary_directory:
            apk = Path(temporary_directory) / "WooCommerce-wasabi-release.apk"
            apk.touch()
            with patch.object(release_app, "find_aapt", return_value=Path("/fake/aapt")), patch.object(
                release_app, "run_command", return_value=badging
            ):
                with self.assertRaisesRegex(release_app.ReleaseAppError, "expected production package"):
                    release_app.validate_release_apk(apk)

    def test_installed_debuggable_production_package_is_identified(self) -> None:
        metadata = release_app.parse_installed_package_dump(
            "  versionName=25.4\n  flags=[ DEBUGGABLE HAS_CODE ]\n"
        )

        self.assertEqual(metadata.version_name, "25.4")
        self.assertTrue(metadata.debuggable)

    def test_existing_release_app_is_reused_without_downloading(self) -> None:
        installed = release_app.ApkMetadata("com.woocommerce.android", "25.4", False)

        with patch.object(release_app, "installed_release", return_value=installed), patch.object(
            release_app, "latest_release_asset"
        ) as latest:
            result = release_app.ensure_release_app("emulator-5554")

        self.assertFalse(result.installed_now)
        self.assertEqual(result.version_name, "25.4")
        latest.assert_not_called()

    def test_missing_release_app_downloads_verifies_and_installs_latest_stable_apk(self) -> None:
        asset = release_app.ReleaseAsset(
            "25.4",
            "WooCommerce-25.4-universal.apk",
            "https://github.com/example/app.apk",
            42,
            "a" * 64,
        )
        metadata = release_app.ApkMetadata("com.woocommerce.android", "25.4", False)

        with tempfile.TemporaryDirectory() as temporary_directory:
            apk = Path(temporary_directory) / asset.name
            apk.touch()
            with patch.object(release_app, "installed_release", side_effect=[None, metadata]), patch.object(
                release_app, "latest_release_asset", return_value=asset
            ), patch.object(release_app, "download_release_asset", return_value=apk), patch.object(
                release_app, "validate_release_apk", return_value=metadata
            ), patch.object(release_app, "install_release_apk") as install:
                result = release_app.ensure_release_app(
                    "emulator-5554",
                    cache_dir=Path(temporary_directory),
                )

        self.assertTrue(result.installed_now)
        self.assertEqual(result.source, "GitHub release 25.4")
        install.assert_called_once_with("emulator-5554", apk)


if __name__ == "__main__":
    unittest.main()
