#!/usr/bin/env python3
"""Tests for the Android device locale pre-flight check."""

from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPT_DIR))

import device_locale  # noqa: E402


def command_result(stdout: str = "", returncode: int = 0) -> subprocess.CompletedProcess[str]:
    return subprocess.CompletedProcess([], returncode, stdout=stdout, stderr="")


class DeviceLocaleTest(unittest.TestCase):
    def test_accepts_english_locale_with_region(self) -> None:
        result = device_locale.ensure_english_device_locale(
            "emulator-5554",
            lambda _command: command_result("en-US\n"),
        )

        self.assertEqual(result.primary, "en-US")
        self.assertEqual(result.source, "cmd locale get-device-locale")

    def test_accepts_english_locale_with_underscore(self) -> None:
        result = device_locale.ensure_english_device_locale(
            "emulator-5554",
            lambda _command: command_result("en_GB\n"),
        )

        self.assertEqual(result.primary, "en-GB")

    def test_rejects_non_english_primary_locale_even_when_english_is_secondary(self) -> None:
        responses = iter(
            (
                command_result(returncode=1),
                command_result(""),
                command_result("es-ES,en-US\n"),
            )
        )

        with self.assertRaisesRegex(
            device_locale.DeviceLocaleError,
            r"primary locale is es-ES; Maestro flows require English",
        ):
            device_locale.ensure_english_device_locale("emulator-5554", lambda _command: next(responses))

    def test_falls_back_to_legacy_persisted_language(self) -> None:
        responses = iter(
            (
                command_result("Locale manager help\n"),
                command_result("null\n"),
                command_result(""),
                command_result("en\n"),
            )
        )

        result = device_locale.ensure_english_device_locale(
            "emulator-5554",
            lambda _command: next(responses),
        )

        self.assertEqual(result, device_locale.DeviceLocale("en", "persist.sys.language"))

    def test_fails_when_no_locale_probe_returns_a_locale(self) -> None:
        with self.assertRaisesRegex(
            device_locale.DeviceLocaleError,
            r"could not determine the primary locale for adb device emulator-5554",
        ):
            device_locale.ensure_english_device_locale(
                "emulator-5554",
                lambda _command: command_result(""),
            )


if __name__ == "__main__":
    unittest.main()
