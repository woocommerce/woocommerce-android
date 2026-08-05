#!/usr/bin/env python3
"""Pre-flight doctor for WooCommerce Android Maestro smoke runs."""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

from smoke_plan import PROFILES, flow_tags, selected_flows


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_ENV_FILE = REPO_ROOT / ".maestro" / ".env.local"
LINT_ENV = SCRIPT_DIR / "lint-env.py"
SEED_SCRIPT = SCRIPT_DIR / "seed-fixtures.py"
CHECK_TOOLCHAIN = SCRIPT_DIR / "check-toolchain.py"
SHARED_STORE_HOST = "inpersonpayments.wpcomstaging.com"

ASSIGNMENT_RE = re.compile(r"^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$")
REF_RE = re.compile(r"\$\{(WOO_[A-Z0-9_]+)\}")
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
        flow_refs = set(REF_RE.findall(text))
        if flow.name == "login_not_woo_store.yaml":
            flow_refs.difference_update(
                {"WOO_NOT_A_WOO_STORE_WPCOM_EMAIL", "WOO_NOT_A_WOO_STORE_WPCOM_PASSWORD"}
            )
        refs.update(flow_refs)
        if SUBFLOW_LOGIN_RE.search(text):
            refs.update({"WOO_JETPACK_STORE_URL", "WOO_WPCOM_EMAIL", "WOO_WPCOM_PASSWORD"})
    if seed:
        refs.update({"WOO_STORE_URL", "WOO_CONSUMER_KEY", "WOO_CONSUMER_SECRET"})
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
        "WOO_NO_JETPACK_SITE_URL": ["MAESTRO_WOO_NO_JETPACK_SITE_URL", "MAESTRO_WOO_JN_SITE_URL"],
        "WOO_NO_JETPACK_SITE_ADMIN_USERNAME": [
            "MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_USERNAME",
            "MAESTRO_WOO_JN_USERNAME",
        ],
        "WOO_NO_JETPACK_SITE_ADMIN_PASSWORD": [
            "MAESTRO_WOO_NO_JETPACK_SITE_ADMIN_PASSWORD",
            "MAESTRO_WOO_JN_PASSWORD",
        ],
    }
    return mapped.get(ref, [f"MAESTRO_{ref}"])


def has_value(env: dict[str, str], names: list[str]) -> bool:
    return any(bool(env.get(name, "")) for name in names)


def url_host(value: str) -> str:
    value = value.removeprefix("http://").removeprefix("https://")
    return value.split("/", 1)[0].split(":", 1)[0].lower()


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


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Maestro smoke-test prerequisites without running flows.")
    parser.add_argument("--profile", choices=sorted(PROFILES), default="core")
    parser.add_argument("--store", choices=("lab", "shared"))
    parser.add_argument("--include-tags")
    parser.add_argument("--exclude-tags")
    parser.add_argument("--include-quarantine", action="store_true")
    parser.add_argument("--device", help="Expected adb serial. AVD-name matching is handled by the runner.")
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV_FILE)
    parser.add_argument("--seed", action="store_true")
    args = parser.parse_args()

    profile = PROFILES[args.profile]
    store = args.store or profile.store
    include_tags = parse_csv(args.include_tags)
    if include_tags is None:
        include_tags = list(profile.include)
    exclude_tags = parse_csv(args.exclude_tags)
    if exclude_tags is None:
        exclude_tags = list(profile.exclude)
    if args.include_quarantine:
        exclude_tags = [tag for tag in exclude_tags if tag != "flaky_quarantine"]

    checks: list[Check] = [
        command_check("bash"),
        command_check("python3"),
        command_check("maestro"),
        command_check("adb"),
        toolchain_check(),
    ]

    if args.env_file.exists():
        lint_command = [str(LINT_ENV), "--file", str(args.env_file)]
        if args.seed:
            lint_command.append("--seed")
        lint = subprocess.run(lint_command, cwd=REPO_ROOT, capture_output=True, text=True)
        checks.append(Check("ok" if lint.returncode == 0 else "fail", f"{args.env_file} lint {'passed' if lint.returncode == 0 else 'failed'}"))
    else:
        checks.append(Check("warn", f"{args.env_file} not found; expecting credentials from exported environment or CI secrets"))

    env = dict(os.environ)
    env.update(parse_env_file(args.env_file))

    flows = selected_flows(include_tags, exclude_tags)
    checks.append(Check("ok" if flows else "fail", f"{len(flows)} flow(s) selected for profile {args.profile}"))

    refs = referenced_env(flows, args.seed)
    missing = sorted(ref for ref in refs if not has_value(env, candidates_for(ref, store)))
    if missing:
        checks.append(Check("fail", "missing required env vars: " + ", ".join("MAESTRO_" + ref for ref in missing)))
    else:
        checks.append(Check("ok", f"all {len(refs)} referenced WOO_* env value(s) are available"))

    if any(flow.name == "login_not_woo_store.yaml" for flow in flows):
        wpcom_fallback = [
            has_value(env, candidates_for("WOO_NOT_A_WOO_STORE_WPCOM_EMAIL", store)),
            has_value(env, candidates_for("WOO_NOT_A_WOO_STORE_WPCOM_PASSWORD", store)),
        ]
        if any(wpcom_fallback) and not all(wpcom_fallback):
            checks.append(Check("fail", "not-Woo-store WP.com fallback requires both email and password"))

    jetpack_candidates = candidates_for("WOO_JETPACK_STORE_URL", store)
    no_jetpack_candidates = candidates_for("WOO_NO_JETPACK_SITE_URL", store)
    jetpack_url = next((env[name] for name in jetpack_candidates if env.get(name)), "")
    no_jetpack_url = next((env[name] for name in no_jetpack_candidates if env.get(name)), "")
    if jetpack_url and no_jetpack_url and url_host(jetpack_url) == url_host(no_jetpack_url):
        checks.append(Check("fail", "selected Jetpack store URL matches the no-Jetpack site URL"))

    has_destructive_flow = any("destructive" in flow_tags(flow) for flow in flows)
    if store == "shared" and has_destructive_flow:
        if not args.seed:
            checks.append(Check("fail", "shared destructive flows require --seed"))
        shared_url = env.get("MAESTRO_WOO_SHARED_JETPACK_STORE_URL", "")
        shared_host = url_host(shared_url)
        if shared_host != SHARED_STORE_HOST:
            checks.append(
                Check(
                    "fail",
                    f"shared destructive host must be {SHARED_STORE_HOST}; "
                    f"configured host is {shared_host or '<empty>'}",
                )
            )
        if not os.access(SEED_SCRIPT, os.X_OK):
            checks.append(Check("fail", f"shared-store lock helper is not executable: {SEED_SCRIPT}"))
        if not os.environ.get("CI") and not os.environ.get("BUILDKITE"):
            checks.append(Check("fail", "shared destructive runs are refused outside CI"))

    devices = adb_devices()
    if args.device:
        checks.append(Check("ok" if args.device in devices else "fail", f"requested adb device {args.device} {'is connected' if args.device in devices else 'is not connected'}"))
    else:
        checks.append(Check("ok" if devices else "fail", f"{len(devices)} adb device(s) connected"))

    print("Maestro smoke doctor")
    print(f"  profile: {args.profile}")
    print(f"  store:   {store}")
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
