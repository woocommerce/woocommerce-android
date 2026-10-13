#!/usr/bin/env python3
"""Pre-flight doctor for WooCommerce Android Maestro smoke runs."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path

from device_locale import DeviceLocaleError, ensure_english_device_locale
from ensure_release_app import ReleaseAppError, ensure_release_app
from smoke_plan import POS_TAG, PROFILES, SEEDING_PROFILES, flow_store, flow_tags, needs_pos_device, selected_flows


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_ENV_FILE = REPO_ROOT / ".maestro" / ".env.local"
LINT_ENV = SCRIPT_DIR / "lint-env.py"
CHECK_TOOLCHAIN = SCRIPT_DIR / "check-toolchain.py"

ASSIGNMENT_RE = re.compile(r"^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$")
REF_RE = re.compile(r"\$\{MAESTRO_(WOO_[A-Z0-9_]+)\}")
SUBFLOW_LOGIN_RE = re.compile(r"subflows/(ensure_logged_in|login)\.yaml")

@dataclass
class Check:
    status: str
    message: str


def parse_csv(value: str | None) -> list[str] | None:
    if value is None:
        return None
    return [item.strip() for item in value.split(",") if item.strip()]


def parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw_line in path.read_text(errors="replace").splitlines():
        stripped = raw_line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        match = ASSIGNMENT_RE.match(stripped)
        if not match:
            continue
        name, value = match.groups()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]
        values[name] = value
    return values


def referenced_env(flows: list[Path], seed: bool) -> set[str]:
    refs: set[str] = set()
    for flow in flows:
        text = flow.read_text(errors="replace")
        refs.update(REF_RE.findall(text))
        if SUBFLOW_LOGIN_RE.search(text):
            refs.update({"WOO_JETPACK_STORE_URL", "WOO_WPCOM_EMAIL", "WOO_WPCOM_PASSWORD"})
    if seed:
        refs.update(
            {
                "WOO_STORE_URL",
                "WOO_CONSUMER_KEY",
                "WOO_CONSUMER_SECRET",
                "WOO_JETPACK_SITE_ADMIN_USERNAME",
                "WOO_APPLICATION_PASSWORD",
            }
        )
    return refs


def candidates_for(ref: str, store: str) -> list[str]:
    upper = store.upper()
    mapped = {
        "WOO_JETPACK_STORE_URL": [
            f"MAESTRO_WOO_{upper}_JETPACK_STORE_URL",
            f"MAESTRO_WOO_{upper}_STORE_URL",
        ],
        "WOO_STORE_URL": [
            f"MAESTRO_WOO_{upper}_JETPACK_STORE_URL",
            f"MAESTRO_WOO_{upper}_STORE_URL",
        ],
        "WOO_WPCOM_EMAIL": [
            f"MAESTRO_WOO_{upper}_WPCOM_EMAIL",
            f"MAESTRO_WOO_{upper}_EMAIL",
        ],
        "WOO_WPCOM_PASSWORD": [
            f"MAESTRO_WOO_{upper}_WPCOM_PASSWORD",
            f"MAESTRO_WOO_{upper}_PASSWORD",
        ],
        "WOO_CONSUMER_KEY": [
            f"MAESTRO_WOO_{upper}_CONSUMER_KEY",
        ],
        "WOO_CONSUMER_SECRET": [
            f"MAESTRO_WOO_{upper}_CONSUMER_SECRET",
        ],
        "WOO_JETPACK_SITE_ADMIN_USERNAME": [f"MAESTRO_WOO_{upper}_JETPACK_SITE_ADMIN_USERNAME"],
        "WOO_APPLICATION_PASSWORD": [f"MAESTRO_WOO_{upper}_APPLICATION_PASSWORD"],
    }
    return mapped.get(ref, [f"MAESTRO_{ref}"])


def has_value(env: dict[str, str], names: list[str]) -> bool:
    return any(bool(env.get(name, "")) for name in names)


def command_check(name: str) -> Check:
    path = shutil.which(name)
    if path:
        return Check("ok", f"{name} found at {path}")
    return Check("fail", f"{name} not found on PATH")


def adb_devices() -> list[str]:
    if not shutil.which("adb"):
        return []
    result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    devices: list[str] = []
    for line in result.stdout.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices


def toolchain_check() -> Check:
    result = subprocess.run(
        [sys.executable, str(CHECK_TOOLCHAIN)],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode == 0:
        return Check("ok", "Maestro toolchain matches the repository pin")
    details = [line.strip() for line in result.stderr.splitlines() if line.strip()]
    message = details[-1] if details else "Maestro toolchain check failed"
    return Check("fail", message)


def lab_store_check(url: str) -> Check:
    """An expired Jurassic Ninja store stops answering, and only the setup skill can replace it."""
    try:
        with urllib.request.urlopen(f"{url.rstrip('/')}/?rest_route=/", timeout=15) as response:
            if "namespaces" in json.load(response):
                return Check("ok", "the lab store responds")
    except (OSError, ValueError):
        pass
    return Check("fail", "the lab store doesn't respond; run the /setup-test-stores skill")


def selection_checks(flows: list[Path], args: argparse.Namespace) -> list[Check]:
    """Report the selections the runner refuses before it runs any flow."""
    destructive = any("destructive" in flow_tags(flow) for flow in flows)
    checks = []
    if destructive and not args.seed:
        checks.append(Check("fail", "destructive flows are selected, so the run needs --seed"))
    if destructive and args.store == "shared":
        checks.append(Check("fail", "destructive flows are refused on the shared store"))
    return checks


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Maestro smoke-test prerequisites without running flows.")
    parser.add_argument("--profile", choices=sorted(PROFILES), default="core")
    parser.add_argument("--store", choices=("lab", "shared"))
    parser.add_argument("--include-tags")
    parser.add_argument("--exclude-tags")
    parser.add_argument("--include-quarantine", action="store_true")
    parser.add_argument("--device", help="Expected adb serial. AVD-name matching is handled by the runner.")
    parser.add_argument("--pos-device", help="Tablet adb serial for the POS flows.")
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV_FILE)
    parser.add_argument("--seed", action="store_true")
    args = parser.parse_args()
    if args.profile in SEEDING_PROFILES:
        args.seed = True

    profile = PROFILES[args.profile]
    include_tags = parse_csv(args.include_tags)
    if include_tags is None:
        include_tags = list(profile.include)
    exclude_tags = parse_csv(args.exclude_tags)
    if exclude_tags is None:
        exclude_tags = list(profile.exclude)
    if args.include_quarantine:
        exclude_tags = [tag for tag in exclude_tags if tag != "quarantine"]

    checks: list[Check] = [
        command_check("bash"),
        command_check("python3"),
        command_check("maestro"),
        command_check("adb"),
        toolchain_check(),
    ]

    if args.env_file.exists():
        lint_command = [str(LINT_ENV), "--file", str(args.env_file)]
        lint = subprocess.run(lint_command, cwd=REPO_ROOT, capture_output=True, text=True)
        checks.append(Check("ok" if lint.returncode == 0 else "fail", f"{args.env_file} lint {'passed' if lint.returncode == 0 else 'failed'}"))
    else:
        checks.append(Check("warn", f"{args.env_file} not found; expecting credentials from the exported environment"))

    env = dict(os.environ)
    env.update(parse_env_file(args.env_file))

    flows = selected_flows(include_tags, exclude_tags)
    checks.append(Check("ok" if flows else "fail", f"{len(flows)} flow(s) selected for profile {args.profile}"))
    checks.extend(selection_checks(flows, args))

    stores = [store for store in ("lab", "shared") if any(flow_store(flow, args.store) == store for flow in flows)]
    for store in stores:
        store_flows = [flow for flow in flows if flow_store(flow, args.store) == store]
        refs = referenced_env(store_flows, args.seed and store == "lab")
        missing = sorted(ref for ref in refs if not has_value(env, candidates_for(ref, store)))
        if missing:
            names = ", ".join(candidates_for(ref, store)[0] for ref in missing)
            checks.append(Check("fail", f"missing required env vars for the {store} store: {names}"))
        else:
            checks.append(
                Check("ok", f"all {len(refs)} referenced WOO_* env value(s) for the {store} store are available")
            )

    lab_url = next((env[name] for name in candidates_for("WOO_JETPACK_STORE_URL", "lab") if env.get(name)), "")
    if "lab" in stores and lab_url:
        checks.append(lab_store_check(lab_url))

    if any("destructive" in flow_tags(flow) and flow_store(flow, args.store) == "shared" for flow in flows):
        checks.append(Check("fail", "destructive flows are refused on the shared store"))

    if flows and args.pos_device and all(POS_TAG in flow_tags(flow) for flow in flows):
        args.device, args.pos_device = args.pos_device, None

    devices = adb_devices()
    run_devices: list[str] = []
    if args.device:
        if args.device in devices:
            run_devices.append(args.device)
            checks.append(Check("ok", f"requested adb device {args.device} is connected"))
        else:
            checks.append(Check("fail", f"requested adb device {args.device} is not connected"))
    elif len(devices) == 1:
        run_devices.append(devices[0])
        checks.append(Check("ok", f"adb device {devices[0]} is connected"))
    elif len(devices) > 1:
        checks.append(Check("fail", "multiple adb devices are connected; pass --device to select one"))
    else:
        checks.append(Check("fail", "no adb devices are connected"))

    if needs_pos_device(flows):
        if not args.pos_device:
            checks.append(Check("fail", "the selection has POS flows, which need a tablet; pass --pos-device"))
        elif args.pos_device in run_devices:
            checks.append(Check("fail", "--pos-device must be a different device than --device"))
        elif args.pos_device in devices:
            run_devices.append(args.pos_device)
            checks.append(Check("ok", f"requested POS device {args.pos_device} is connected"))
        else:
            checks.append(Check("fail", f"requested POS device {args.pos_device} is not connected"))

    for device in run_devices:
        try:
            locale = ensure_english_device_locale(device)
            checks.append(Check("ok", locale.message))
        except DeviceLocaleError as error:
            checks.append(Check("fail", str(error)))

    if run_devices and not any(check.status == "fail" for check in checks):
        for device in run_devices:
            try:
                release = ensure_release_app(device)
                checks.append(Check("ok", release.message))
            except ReleaseAppError as error:
                checks.append(Check("fail", str(error)))
    else:
        checks.append(Check("warn", "production release app check skipped because another pre-flight check failed"))

    print("Maestro smoke doctor")
    print(f"  profile: {args.profile}")
    print(f"  store:   {args.store or 'per flow'}")
    print(f"  include: {','.join(include_tags) or '<none>'}")
    print(f"  exclude: {','.join(exclude_tags) or '<none>'}")
    print(f"  seed:    {'yes' if args.seed else 'no'}")
    print()

    failed = 0
    for check in checks:
        marker = {"ok": "OK", "warn": "WARN", "fail": "FAIL"}[check.status]
        print(f"[{marker}] {check.message}")
        failed += int(check.status == "fail")

    if flows:
        print()
        print("Selected flows:")
        for flow in flows:
            print(f"  - {flow.relative_to(REPO_ROOT)}")

    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
