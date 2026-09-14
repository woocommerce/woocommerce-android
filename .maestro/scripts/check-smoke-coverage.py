#!/usr/bin/env python3
"""Offline coverage check for Maestro smoke flows."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


ID_RE = re.compile(r"^\s*-\s+id:\s*([A-Za-z0-9_.-]+)\s*$")
FLOW_RE = re.compile(r"^\s+flow:\s*(.+?)\s*$")
MANUAL_RE = re.compile(r"^\s+manual:\s*(.+?)\s*$")
P2_RE = re.compile(r"^#\s*p2:\s*(.+?)\s*$", re.IGNORECASE)
P2_PROBE_RE = re.compile(r"^#\s*p2\s+probe:\s*(.+?)\s*$", re.IGNORECASE)


def parse_header_ids(value: str) -> set[str]:
    ids = re.split(r"\s+\(", value, maxsplit=1)[0]
    return {item.strip() for item in ids.split(",") if item.strip()}


def parse_snapshot(path: Path) -> tuple[dict[str, dict[str, str]], set[str]]:
    items: dict[str, dict[str, str]] = {}
    duplicates: set[str] = set()
    current: str | None = None
    for line in path.read_text(encoding="utf-8").splitlines():
        id_match = ID_RE.match(line)
        if id_match:
            current = id_match.group(1)
            if current in items:
                duplicates.add(current)
            items[current] = {}
            continue
        if current is None:
            continue
        flow_match = FLOW_RE.match(line)
        if flow_match:
            items[current]["flow"] = flow_match.group(1).strip().strip('"')
            continue
        manual_match = MANUAL_RE.match(line)
        if manual_match:
            items[current]["manual"] = manual_match.group(1).strip().strip('"')
            continue
    return items, duplicates


def parse_flow_headers(flows_dir: Path) -> tuple[dict[str, set[str]], dict[str, set[str]]]:
    claims: dict[str, set[str]] = {}
    probes: dict[str, set[str]] = {}
    for flow in sorted(flows_dir.glob("*.yaml")):
        flow_claims: set[str] = set()
        flow_probes: set[str] = set()
        for line in flow.read_text(encoding="utf-8").splitlines()[:40]:
            probe_match = P2_PROBE_RE.match(line)
            if probe_match:
                flow_probes.update(parse_header_ids(probe_match.group(1)))
                continue
            claim_match = P2_RE.match(line)
            if claim_match:
                flow_claims.update(parse_header_ids(claim_match.group(1)))
        claims[str(flow)] = flow_claims
        probes[str(flow)] = flow_probes
    return claims, probes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--coverage", default=".maestro/smoke-coverage.yaml", type=Path)
    parser.add_argument("--flows-dir", default=".maestro/flows", type=Path)
    args = parser.parse_args()

    items, duplicates = parse_snapshot(args.coverage)
    flow_claims, flow_probes = parse_flow_headers(args.flows_dir)
    errors: list[str] = []

    for item_id in sorted(duplicates):
        errors.append(f"{args.coverage}: duplicate item id {item_id}")

    for item_id, data in sorted(items.items()):
        has_flow = bool(data.get("flow"))
        has_manual = bool(data.get("manual"))
        if has_flow and has_manual:
            errors.append(f"{args.coverage}: item {item_id} has both flow and manual reason")
        elif not has_flow and not has_manual:
            errors.append(f"{args.coverage}: item {item_id} has neither flow nor manual reason")

    known_ids = set(items)
    for flow in flow_claims:
        claims = flow_claims[flow]
        probes = flow_probes[flow]
        if not claims and not probes:
            errors.append(f"{flow}: missing '# p2:' or '# P2 probe:' header")
        for item_id in sorted((claims | probes) - known_ids):
            errors.append(f"{flow}: unknown p2 id {item_id}")

        for item_id in sorted(claims & known_ids):
            mapped_flow = items[item_id].get("flow", "")
            if mapped_flow != flow:
                errors.append(f"{flow}: p2 id {item_id} maps to {mapped_flow or 'manual coverage'}")

        for item_id in sorted(probes & known_ids):
            if items[item_id].get("flow"):
                errors.append(f"{flow}: probe id {item_id} is counted as automated coverage")

    for item_id, data in sorted(items.items()):
        flow = data.get("flow", "")
        if flow and item_id not in flow_claims.get(flow, set()):
            errors.append(f"{args.coverage}: item {item_id} maps to {flow}, but that flow does not declare it")

    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Coverage snapshot OK: {len(items)} items, {len(flow_claims)} flows")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
