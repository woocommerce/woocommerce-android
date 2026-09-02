#!/usr/bin/env python3
"""Ensure a production WooCommerce release APK is installed on an Android device."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_CACHE_DIR = REPO_ROOT / "build" / "maestro-release-apk"
LATEST_RELEASE_API = "https://api.github.com/repos/woocommerce/woocommerce-android/releases/latest"
PRODUCTION_APP_ID = "com.woocommerce.android"


class ReleaseAppError(RuntimeError):
    """Raised when the production release app cannot be validated or installed."""


@dataclass(frozen=True)
class ReleaseAsset:
    tag: str
    name: str
    download_url: str
    size: int
    sha256: str


@dataclass(frozen=True)
class ApkMetadata:
    package_name: str
    version_name: str
    debuggable: bool


@dataclass(frozen=True)
class InstalledRelease:
    version_name: str
    installed_now: bool
    source: str

    @property
    def message(self) -> str:
        action = "installed" if self.installed_now else "already installed"
        return f"production release app {PRODUCTION_APP_ID} {self.version_name} {action} ({self.source})"


def run_command(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, capture_output=True, text=True, check=False)


def adb(device: str, *args: str) -> subprocess.CompletedProcess[str]:
    return run_command(["adb", "-s", device, *args])


def parse_installed_package_dump(package_dump: str) -> ApkMetadata:
    version_match = re.search(r"^\s*versionName=(\S+)", package_dump, re.MULTILINE)
    if not version_match:
        raise ReleaseAppError(f"could not read {PRODUCTION_APP_ID} versionName from adb")
    debuggable = any(
        "DEBUGGABLE" in flags
        for flags in re.findall(r"(?:pkgFlags|flags)=\[([^]]*)]", package_dump)
    )
    return ApkMetadata(PRODUCTION_APP_ID, version_match.group(1), debuggable)


def installed_release(device: str) -> ApkMetadata | None:
    package_path = adb(device, "shell", "pm", "path", PRODUCTION_APP_ID)
    if package_path.returncode != 0 or not any(
        line.startswith("package:") for line in package_path.stdout.splitlines()
    ):
        return None

    package_dump = adb(device, "shell", "dumpsys", "package", PRODUCTION_APP_ID)
    if package_dump.returncode != 0:
        raise ReleaseAppError(f"could not inspect installed package {PRODUCTION_APP_ID}")
    metadata = parse_installed_package_dump(package_dump.stdout)
    if metadata.debuggable:
        raise ReleaseAppError(
            f"{PRODUCTION_APP_ID} is installed but debuggable; uninstall it or replace it with a release APK"
        )
    return metadata


def parse_release_asset(release: dict[str, Any]) -> ReleaseAsset:
    tag = str(release.get("tag_name", "")).strip()
    if not tag:
        raise ReleaseAppError("GitHub latest release response has no tag_name")
    if release.get("draft") or release.get("prerelease"):
        raise ReleaseAppError(f"GitHub latest release {tag} is not a stable published release")

    candidates = [
        asset
        for asset in release.get("assets", [])
        if str(asset.get("name", "")).endswith("-universal.apk")
    ]
    if len(candidates) != 1:
        raise ReleaseAppError(f"GitHub release {tag} must contain exactly one universal APK asset")

    asset = candidates[0]
    name = str(asset.get("name", ""))
    if Path(name).name != name:
        raise ReleaseAppError(f"GitHub release {tag} returned an unsafe APK asset name")
    digest = str(asset.get("digest", ""))
    if not re.fullmatch(r"sha256:[0-9a-fA-F]{64}", digest):
        raise ReleaseAppError(f"GitHub release {tag} universal APK has no verifiable SHA-256 digest")
    download_url = str(asset.get("browser_download_url", ""))
    size = asset.get("size")
    if not download_url.startswith("https://github.com/") or not isinstance(size, int) or size <= 0:
        raise ReleaseAppError(f"GitHub release {tag} universal APK metadata is incomplete")
    return ReleaseAsset(tag, name, download_url, size, digest.split(":", maxsplit=1)[1].lower())


def github_headers() -> dict[str, str]:
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "woocommerce-android-maestro-doctor",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    token = os.environ.get("GITHUB_TOKEN", "")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def latest_release_asset() -> ReleaseAsset:
    request = urllib.request.Request(LATEST_RELEASE_API, headers=github_headers())
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            release = json.load(response)
    except (OSError, ValueError) as error:
        raise ReleaseAppError(f"could not resolve the latest WooCommerce Android release: {error}") from error
    if not isinstance(release, dict):
        raise ReleaseAppError("GitHub latest release response is not an object")
    return parse_release_asset(release)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def valid_cached_asset(path: Path, asset: ReleaseAsset) -> bool:
    return path.is_file() and path.stat().st_size == asset.size and sha256(path) == asset.sha256


def download_release_asset(asset: ReleaseAsset, cache_dir: Path) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)
    destination = cache_dir / asset.name
    if valid_cached_asset(destination, asset):
        return destination

    partial = destination.with_suffix(destination.suffix + ".part")
    partial.unlink(missing_ok=True)
    request = urllib.request.Request(
        asset.download_url,
        headers={"User-Agent": "woocommerce-android-maestro-doctor"},
    )
    try:
        with urllib.request.urlopen(request, timeout=300) as response, partial.open("wb") as output:
            shutil.copyfileobj(response, output, length=1024 * 1024)
        if not valid_cached_asset(partial, asset):
            raise ReleaseAppError(f"downloaded {asset.name} does not match GitHub's size and SHA-256 digest")
        partial.replace(destination)
    except (OSError, ReleaseAppError) as error:
        partial.unlink(missing_ok=True)
        if isinstance(error, ReleaseAppError):
            raise
        raise ReleaseAppError(f"could not download {asset.name}: {error}") from error
    return destination


def build_tools_sort_key(path: Path) -> tuple[int, ...]:
    return tuple(int(part) for part in re.findall(r"\d+", path.parent.name))


def find_aapt() -> Path:
    from_path = shutil.which("aapt")
    if from_path:
        return Path(from_path)
    sdk_root_value = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk_root_value:
        candidates = list((Path(sdk_root_value) / "build-tools").glob("*/aapt"))
        if candidates:
            stable_candidates = [candidate for candidate in candidates if "-" not in candidate.parent.name]
            return max(stable_candidates or candidates, key=build_tools_sort_key)
    raise ReleaseAppError("aapt not found; set ANDROID_HOME or add Android build-tools to PATH")


def parse_apk_badging(output: str) -> ApkMetadata:
    package_match = re.search(r"^package: name='([^']+)'.*versionName='([^']+)'", output, re.MULTILINE)
    if not package_match:
        raise ReleaseAppError("could not read APK package name and version with aapt")
    return ApkMetadata(
        package_name=package_match.group(1),
        version_name=package_match.group(2),
        debuggable="application-debuggable" in output,
    )


def validate_release_apk(path: Path) -> ApkMetadata:
    if not path.is_file() or path.suffix.lower() != ".apk":
        raise ReleaseAppError(f"APK not found: {path}")
    result = run_command([str(find_aapt()), "dump", "badging", str(path)])
    if result.returncode != 0:
        raise ReleaseAppError(f"aapt could not inspect APK: {path}")
    metadata = parse_apk_badging(result.stdout)
    if metadata.package_name != PRODUCTION_APP_ID:
        raise ReleaseAppError(
            f"APK package is {metadata.package_name}; expected production package {PRODUCTION_APP_ID}"
        )
    if metadata.debuggable:
        raise ReleaseAppError(f"APK {path.name} is debuggable; a production release APK is required")
    return metadata


def install_release_apk(device: str, path: Path) -> None:
    result = adb(device, "install", "-r", "-g", str(path))
    if result.returncode != 0:
        details = [line.strip() for line in (result.stderr + result.stdout).splitlines() if line.strip()]
        raise ReleaseAppError(details[-1] if details else f"adb could not install {path.name}")


def ensure_release_app(
    device: str,
    cache_dir: Path = DEFAULT_CACHE_DIR,
    apk_path: Path | None = None,
) -> InstalledRelease:
    if apk_path is not None:
        metadata = validate_release_apk(apk_path)
        install_release_apk(device, apk_path)
        installed = installed_release(device)
        if installed is None or installed.version_name != metadata.version_name:
            raise ReleaseAppError(f"installed {PRODUCTION_APP_ID} does not match supplied APK {metadata.version_name}")
        return InstalledRelease(installed.version_name, True, f"supplied APK {apk_path.name}")

    installed = installed_release(device)
    if installed is not None:
        return InstalledRelease(installed.version_name, False, "device")

    asset = latest_release_asset()
    print(f"Downloading WooCommerce Android {asset.tag} production APK from GitHub Releases...")
    downloaded = download_release_asset(asset, cache_dir)
    metadata = validate_release_apk(downloaded)
    install_release_apk(device, downloaded)
    installed = installed_release(device)
    if installed is None or installed.version_name != metadata.version_name:
        raise ReleaseAppError(f"installed {PRODUCTION_APP_ID} does not match downloaded release {asset.tag}")
    return InstalledRelease(installed.version_name, True, f"GitHub release {asset.tag}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True, help="adb device serial")
    parser.add_argument("--apk", type=Path, help="production release APK to validate and install")
    parser.add_argument("--cache-dir", type=Path, default=DEFAULT_CACHE_DIR)
    args = parser.parse_args()

    try:
        release = ensure_release_app(args.device, args.cache_dir, args.apk)
    except ReleaseAppError as error:
        print(f"Production release app check failed: {error}", file=sys.stderr)
        return 1
    print(release.message)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
