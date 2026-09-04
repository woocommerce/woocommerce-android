#!/usr/bin/env python3
"""Canonical profile and flow-selection policy for Android Maestro smoke tests."""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
FLOWS_DIR = REPO_ROOT / ".maestro" / "flows"


@dataclass(frozen=True)
class Profile:
    store: str
    include: tuple[str, ...]
    exclude: tuple[str, ...]
    repeat: int = 1


PROFILES = {
    "core": Profile("lab", ("smoke_core",), ("flaky_quarantine", "android_system")),
    "phone-full": Profile(
        "lab",
        ("smoke_core", "smoke_extended"),
        ("pos_tablet", "android_system"),
    ),
    "release": Profile(
        "shared",
        ("smoke_core", "smoke_extended", "destructive"),
        ("flaky_quarantine", "pos_tablet", "android_system"),
    ),
    "burst": Profile(
        "shared",
        ("smoke_core", "smoke_extended", "destructive"),
        ("flaky_quarantine", "pos_tablet", "android_system"),
        repeat=3,
    ),
    "pos-tablet": Profile("lab", ("pos_tablet",), ()),
    "android-system": Profile("lab", ("android_system",), ()),
}

P2_ORDERED_FLOW_NAMES = (
    "login_not_wp_site.yaml",
    "login_wrong_credentials.yaml",
    "login_help.yaml",
    "login_not_woo_store.yaml",
    "login_wrong_account.yaml",
    "login_no_jetpack.yaml",
    "login_google.yaml",
    "login_successful.yaml",
    "dashboard_stats.yaml",
    "dashboard_view_all_analytics.yaml",
    "dashboard_customize.yaml",
    "orders_list_and_search.yaml",
    "orders_create.yaml",
    "orders_details_and_actions.yaml",
    "orders_mark_complete.yaml",
    "orders_cash_payment.yaml",
    "orders_barcode_scanner_opens.yaml",
    "orders_payment_qr_and_share.yaml",
    "orders_refund.yaml",
    "products_list_and_sort.yaml",
    "products_detail.yaml",
    "products_variations_and_tags.yaml",
    "products_create.yaml",
    "products_media_upload.yaml",
    "hub_menu_settings.yaml",
    "hub_menu_payments.yaml",
    "hub_menu_coupons.yaml",
    "hub_menu_customers_inbox.yaml",
    "hub_menu_admin_and_store.yaml",
    "blaze_campaign.yaml",
    "google_for_woo.yaml",
    "pos_search_and_coupons.yaml",
    "pos_cash_payment.yaml",
    "android_quick_actions.yaml",
)


def parse_csv(value: str) -> tuple[str, ...]:
    return tuple(item.strip() for item in value.split(",") if item.strip())


def flow_tags(path: Path) -> frozenset[str]:
    tags: set[str] = set()
    in_tags = False
    header = path.read_text(errors="replace").split("---", 1)[0]
    for line in header.splitlines():
        if line.strip() == "tags:":
            in_tags = True
            continue
        if in_tags and line.lstrip().startswith("-"):
            tags.add(line.split("-", 1)[1].strip())
        elif in_tags and line and not line.startswith((" ", "\t")):
            in_tags = False
    return frozenset(tags)


def selected_flows(
    include_tags: tuple[str, ...],
    exclude_tags: tuple[str, ...],
    flows_dir: Path = FLOWS_DIR,
) -> tuple[Path, ...]:
    selected: list[Path] = []
    for name in P2_ORDERED_FLOW_NAMES:
        path = flows_dir / name
        if not path.exists():
            continue
        tags = flow_tags(path)
        if include_tags and tags.isdisjoint(include_tags):
            continue
        if exclude_tags and not tags.isdisjoint(exclude_tags):
            continue
        selected.append(path)
    return tuple(selected)


def print_profile(name: str) -> None:
    profile = PROFILES[name]
    print(f"store\t{profile.store}")
    print(f"repeat\t{profile.repeat}")
    print(f"include\t{','.join(profile.include)}")
    print(f"exclude\t{','.join(profile.exclude)}")


def print_plan(args: argparse.Namespace) -> int:
    include_tags = parse_csv(args.include_tags)
    exclude_tags = parse_csv(args.exclude_tags)
    flows = selected_flows(include_tags, exclude_tags)
    if not flows:
        print("No flows matched the current filters.", file=sys.stderr)
        return 1

    print("Maestro smoke plan")
    print(f"  profile: {args.profile_label or '<custom>'}")
    print(f"  store:   {args.store}")
    print(f"  repeat:  {args.repeat}")
    print(f"  include: {','.join(include_tags) or '<none>'}")
    print(f"  exclude: {','.join(exclude_tags) or '<none>'}")
    print(f"  seed:    {'yes' if args.seed else 'no'}")
    print(f"  flows:   {len(flows)}")
    print()
    print("Selected flows:")
    for flow in flows:
        print(f"  - {flow.relative_to(REPO_ROOT)}")
    return 0


def print_selection(args: argparse.Namespace) -> int:
    flows = selected_flows(parse_csv(args.include_tags), parse_csv(args.exclude_tags))
    if not flows:
        print("No flows matched the current filters.", file=sys.stderr)
        return 1
    for flow in flows:
        print(flow)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    profile_parser = subparsers.add_parser("profile")
    profile_parser.add_argument("name", choices=sorted(PROFILES))

    plan_parser = subparsers.add_parser("plan")
    plan_parser.add_argument("--profile-label", default="")
    plan_parser.add_argument("--store", choices=("lab", "shared"), required=True)
    plan_parser.add_argument("--repeat", type=int, required=True)
    plan_parser.add_argument("--include-tags", default="")
    plan_parser.add_argument("--exclude-tags", default="")
    plan_parser.add_argument("--seed", action="store_true")

    select_parser = subparsers.add_parser("select")
    select_parser.add_argument("--include-tags", default="")
    select_parser.add_argument("--exclude-tags", default="")

    args = parser.parse_args()
    if args.command == "profile":
        print_profile(args.name)
        return 0
    if args.command == "select":
        return print_selection(args)
    return print_plan(args)


if __name__ == "__main__":
    raise SystemExit(main())
