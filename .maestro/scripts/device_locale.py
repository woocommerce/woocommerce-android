#!/usr/bin/env python3
"""Validate that an Android device uses English as its primary locale."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from typing import Callable


LOCALE_RE = re.compile(r"^[A-Za-z]{2,3}(?:[-_][A-Za-z0-9]{1,8})*$")


class DeviceLocaleError(RuntimeError):
    """Raised when the device locale cannot be read or is not English."""


@dataclass(frozen=True)
class DeviceLocale:
    primary: str
    source: str

    @property
    def message(self) -> str:
        return f"device primary locale {self.primary} is English ({self.source})"


CommandRunner = Callable[[list[str]], subprocess.CompletedProcess[str]]


def run_command(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, capture_output=True, text=True, check=False)


def parse_primary_locale(value: str) -> str | None:
    candidate = value.strip().split(",", maxsplit=1)[0].strip()
    if not candidate or candidate.lower() == "null" or not LOCALE_RE.fullmatch(candidate):
        return None
    return candidate.replace("_", "-")


def read_device_locale(device: str, command_runner: CommandRunner = run_command) -> DeviceLocale:
    probes = (
        ("cmd locale get-device-locale", ["shell", "cmd", "locale", "get-device-locale"]),
        ("persist.sys.locale", ["shell", "getprop", "persist.sys.locale"]),
        ("system_locales", ["shell", "settings", "get", "system", "system_locales"]),
        ("persist.sys.language", ["shell", "getprop", "persist.sys.language"]),
    )
    for source, arguments in probes:
        result = command_runner(["adb", "-s", device, *arguments])
        if result.returncode != 0:
            continue
        primary = parse_primary_locale(result.stdout)
        if primary:
            return DeviceLocale(primary=primary, source=source)
    raise DeviceLocaleError(f"could not determine the primary locale for adb device {device}")


def ensure_english_device_locale(
    device: str,
    command_runner: CommandRunner = run_command,
) -> DeviceLocale:
    locale = read_device_locale(device, command_runner)
    language = locale.primary.split("-", maxsplit=1)[0].lower()
    if language != "en":
        raise DeviceLocaleError(
            f"device {device} primary locale is {locale.primary}; Maestro flows require English. "
            "Set Settings > System > Languages > System languages to English, then rerun pre-flight"
        )
    return locale


def main() -> int:
    parser = argparse.ArgumentParser(description="Require an English primary locale on an Android device.")
    parser.add_argument("--device", required=True, help="adb device serial")
    args = parser.parse_args()

    try:
        locale = ensure_english_device_locale(args.device)
    except DeviceLocaleError as error:
        print(f"Setup error: {error}", file=sys.stderr)
        return 1
    print(locale.message)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
