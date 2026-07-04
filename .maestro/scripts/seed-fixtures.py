#!/usr/bin/env python3
"""Seed and clean WooCommerce entities for Maestro smoke tests.

The script intentionally lives outside Maestro YAML. Setup failures should stop
the suite before any flow starts, not appear as UI-test failures.
"""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any


CONSUMABLE_MULTIPLIER = 2
RUN_ID_RE = re.compile(r"^SUITE-\d{8,14}-[A-Za-z0-9]+$")
ORPHAN_AGE_HOURS = 48
LOCK_TTL_SECONDS = 60 * 60
API_PREFIX = "/wp-json/wc/v3/"


class SmokeSetupError(RuntimeError):
    pass


def utc_now() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def parse_wc_date(value: str | None) -> dt.datetime | None:
    if not value:
        return None
    normalized = value.replace("Z", "+00:00")
    try:
        parsed = dt.datetime.fromisoformat(normalized)
    except ValueError:
        return None
    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=dt.timezone.utc)
    return parsed.astimezone(dt.timezone.utc)


def strict_run_id(value: str) -> str:
    if not RUN_ID_RE.match(value):
        raise SmokeSetupError(
            f"Invalid SUITE_RUN_ID {value!r}; expected SUITE-<date>-<hash>."
        )
    return value


def env_required(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SmokeSetupError(f"Missing required environment variable: {name}")
    return value


def load_store_env(store: str) -> None:
    prefix = f"MAESTRO_WOO_{store.upper()}_"
    for suffix in ("STORE_URL", "CONSUMER_KEY", "CONSUMER_SECRET", "EMAIL", "PASSWORD"):
        scoped = os.environ.get(prefix + suffix, "").strip()
        if scoped:
            os.environ[f"MAESTRO_WOO_{suffix}"] = scoped


class WooClient:
    def __init__(self) -> None:
        self.site_url = env_required("MAESTRO_WOO_STORE_URL").rstrip("/")
        self.consumer_key = env_required("MAESTRO_WOO_CONSUMER_KEY")
        self.consumer_secret = env_required("MAESTRO_WOO_CONSUMER_SECRET")

    def request(
        self,
        method: str,
        path: str,
        body: dict[str, Any] | None = None,
        query: dict[str, Any] | None = None,
    ) -> Any:
        url = self.site_url + API_PREFIX + path.lstrip("/")
        if query:
            url += "?" + urllib.parse.urlencode(query, doseq=True)
        data = None
        headers = {
            "Accept": "application/json",
            "User-Agent": "woocommerce-android-maestro-smoke",
        }
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        token = f"{self.consumer_key}:{self.consumer_secret}".encode("utf-8")
        headers["Authorization"] = "Basic " + base64.b64encode(token).decode("ascii")
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                raw = response.read().decode("utf-8")
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise SmokeSetupError(f"WooCommerce API {method} {path} failed: {exc.code} {detail}") from exc
        except urllib.error.URLError as exc:
            raise SmokeSetupError(f"WooCommerce API {method} {path} failed: {exc.reason}") from exc

    def create(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        return self.request("POST", path, payload)

    def delete(self, path: str, entity_id: int) -> None:
        self.request("DELETE", f"{path}/{entity_id}", query={"force": "true"})

    def list(self, path: str, **query: Any) -> list[dict[str, Any]]:
        query.setdefault("per_page", 100)
        return self.request("GET", path, query=query)


def manifest_template(run_id: str, store: str) -> dict[str, Any]:
    return {
        "run_id": run_id,
        "store": store,
        "created_at": utc_now().isoformat(),
        "entities": [],
        "env": {},
        "sweep_deletions": [],
        "lock": None,
    }


def record(manifest: dict[str, Any], entity_type: str, entity_id: int, label: str) -> None:
    manifest["entities"].append({"type": entity_type, "id": entity_id, "label": label})


def suite_email(run_id: str) -> str:
    safe = re.sub(r"[^A-Za-z0-9]", "", run_id).lower()
    return f"suite-{safe}@example.invalid"


def seed(args: argparse.Namespace) -> None:
    run_id = strict_run_id(args.run_id)
    load_store_env(args.store)
    client = WooClient()
    manifest_path = Path(args.manifest)
    if manifest_path.exists():
        manifest = read_json(manifest_path)
        manifest["run_id"] = run_id
        manifest["store"] = args.store
        manifest.setdefault("entities", [])
        manifest.setdefault("sweep_deletions", [])
    else:
        manifest = manifest_template(run_id, args.store)
    customer_name = f"{run_id} Tester"
    product_name = f"{run_id} Variable Product"
    simple_product_name = f"{run_id} Simple Product"
    coupon_code = f"{run_id}-10".upper()

    tag = client.create("products/tags", {"name": f"{run_id} Tag"})
    record(manifest, "product_tag", int(tag["id"]), "variable product tag")

    simple_product = client.create(
        "products",
        {
            "name": simple_product_name,
            "type": "simple",
            "regular_price": "12.00",
            "sku": f"{run_id}-simple",
            "manage_stock": True,
            "stock_quantity": 20,
            "tags": [{"id": tag["id"]}],
        },
    )
    record(manifest, "product", int(simple_product["id"]), "simple order product")

    variable_product = client.create(
        "products",
        {
            "name": product_name,
            "type": "variable",
            "sku": f"{run_id}-variable",
            "tags": [{"id": tag["id"]}],
            "attributes": [
                {
                    "name": "Size",
                    "visible": True,
                    "variation": True,
                    "options": ["Small", "Large"],
                }
            ],
        },
    )
    record(manifest, "product", int(variable_product["id"]), "variable product")
    for option in ("Small", "Large"):
        variation = client.create(
            f"products/{variable_product['id']}/variations",
            {
                "regular_price": "12.00",
                "sku": f"{run_id}-variable-{option.lower()}",
                "attributes": [{"name": "Size", "option": option}],
            },
        )
        record(manifest, "product_variation", int(variation["id"]), f"variation {option}")

    customer = client.create(
        "customers",
        {
            "email": suite_email(run_id),
            "first_name": run_id,
            "last_name": "Tester",
            "username": re.sub(r"[^A-Za-z0-9]", "", run_id.lower()),
            "billing": {
                "first_name": run_id,
                "last_name": "Tester",
                "email": suite_email(run_id),
                "country": "US",
            },
        },
    )
    record(manifest, "customer", int(customer["id"]), "known customer")

    coupon = client.create(
        "coupons",
        {
            "code": coupon_code,
            "discount_type": "percent",
            "amount": "10",
            "description": f"{run_id} smoke coupon",
        },
    )
    record(manifest, "coupon", int(coupon["id"]), "active coupon")

    pending_order_ids: list[int] = []
    refundable_order_ids: list[int] = []
    for index in range(1, CONSUMABLE_MULTIPLIER + 1):
        pending = create_order(
            client=client,
            run_id=run_id,
            customer=customer,
            product_id=int(simple_product["id"]),
            status="pending",
            set_paid=False,
            label=f"pending-order-{index}",
        )
        pending_order_ids.append(int(pending["id"]))
        record(manifest, "order", int(pending["id"]), f"pending-order-{index}")

        refundable = create_order(
            client=client,
            run_id=run_id,
            customer=customer,
            product_id=int(simple_product["id"]),
            status="completed",
            set_paid=True,
            label=f"refundable-order-{index}",
        )
        refundable_order_ids.append(int(refundable["id"]))
        record(manifest, "order", int(refundable["id"]), f"refundable-order-{index}")

    manifest["env"] = {
        "MAESTRO_SUITE_RUN_ID": run_id,
        "MAESTRO_FIXTURE_CUSTOMER_NAME": customer_name,
        "MAESTRO_FIXTURE_CUSTOMER_EMAIL": suite_email(run_id),
        "MAESTRO_FIXTURE_VARIABLE_PRODUCT": product_name,
        "MAESTRO_FIXTURE_SIMPLE_PRODUCT": simple_product_name,
        "MAESTRO_FIXTURE_COUPON_CODE": coupon_code,
        "MAESTRO_FIXTURE_PENDING_ORDER_ID": str(pending_order_ids[0]),
        "MAESTRO_FIXTURE_PENDING_ORDER_IDS": ",".join(str(item) for item in pending_order_ids),
        "MAESTRO_FIXTURE_REFUNDABLE_ORDER_ID": str(refundable_order_ids[0]),
        "MAESTRO_FIXTURE_REFUNDABLE_ORDER_IDS": ",".join(str(item) for item in refundable_order_ids),
    }

    write_json(args.manifest, manifest)
    if args.env_file:
        write_env_file(Path(args.env_file), manifest["env"])
    print(f"Seeded {len(manifest['entities'])} entities for {run_id}")


def create_order(
    client: WooClient,
    run_id: str,
    customer: dict[str, Any],
    product_id: int,
    status: str,
    set_paid: bool,
    label: str,
) -> dict[str, Any]:
    return client.create(
        "orders",
        {
            "status": status,
            "set_paid": set_paid,
            "customer_id": customer["id"],
            "payment_method": "cod",
            "payment_method_title": "Cash on delivery",
            "billing": {
                "first_name": run_id,
                "last_name": "Tester",
                "email": customer["email"],
                "country": "US",
            },
            "shipping": {
                "first_name": run_id,
                "last_name": "Tester",
                "country": "US",
            },
            "customer_note": f"{run_id} {label}",
            "line_items": [{"product_id": product_id, "quantity": 1}],
            "meta_data": [{"key": "suite_run_id", "value": run_id}],
        },
    )


def cleanup(args: argparse.Namespace) -> None:
    manifest = read_json(Path(args.manifest))
    load_store_env(manifest.get("store", args.store or "lab"))
    client = WooClient()
    errors: list[str] = []
    type_to_path = {
        "order": "orders",
        "coupon": "coupons",
        "product_variation": None,
        "product": "products",
        "product_tag": "products/tags",
        "customer": "customers",
        "lock_product": "products",
    }
    for entity in reversed(manifest.get("entities", [])):
        entity_type = entity.get("type")
        path = type_to_path.get(entity_type)
        if path is None:
            continue
        try:
            client.delete(path, int(entity["id"]))
        except SmokeSetupError as exc:
            errors.append(str(exc))
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        raise SmokeSetupError(f"Cleanup completed with {len(errors)} deletion error(s).")
    print(f"Cleaned {len(manifest.get('entities', []))} manifest entities")


def sweep(args: argparse.Namespace) -> None:
    load_store_env(args.store)
    client = WooClient()
    deleted: list[dict[str, Any]] = []
    candidates: list[tuple[str, str, dict[str, Any]]] = []
    for entity_type, path in (
        ("product", "products"),
        ("coupon", "coupons"),
        ("order", "orders"),
        ("customer", "customers"),
    ):
        query: dict[str, Any] = {"search": "SUITE-"}
        if entity_type in {"product", "order"}:
            query["status"] = "any"
        for item in client.list(path, **query):
            candidates.append((entity_type, path, item))

    for entity_type, path, item in candidates:
        label = entity_label(entity_type, item)
        match = re.search(r"SUITE-\d{8,14}-[A-Za-z0-9]+", label)
        if "SUITE-" in label and not match:
            raise SmokeSetupError(
                f"Orphan sweep refused loose automation match for {entity_type} {item.get('id')}: {label!r}"
            )
        if not match:
            continue
        created = parse_wc_date(item.get("date_created_gmt") or item.get("date_created"))
        if created is None:
            continue
        age = utc_now() - created
        if age < dt.timedelta(hours=ORPHAN_AGE_HOURS):
            continue
        record_item = {
            "type": entity_type,
            "id": item.get("id"),
            "label": label,
            "age_hours": round(age.total_seconds() / 3600, 1),
            "dry_run": args.dry_run,
        }
        deleted.append(record_item)
        if not args.dry_run:
            client.delete(path, int(item["id"]))

    if args.report:
        write_json(Path(args.report), {"deleted": deleted, "dry_run": args.dry_run})
    action = "Would delete" if args.dry_run else "Deleted"
    print(f"{action} {len(deleted)} stale automation orphan(s)")


def lock(args: argparse.Namespace) -> None:
    run_id = strict_run_id(args.run_id)
    load_store_env(args.store)
    client = WooClient()
    locks = client.list("products", search="SUITE-LOCK-", status="any")
    now = utc_now()
    for item in locks:
        name = str(item.get("name", ""))
        if not name.startswith("SUITE-LOCK-"):
            continue
        created = parse_wc_date(item.get("date_created_gmt") or item.get("date_created"))
        expired = created is None or (now - created).total_seconds() > args.ttl_seconds
        if expired:
            print(f"Deleting expired shared-store lock product {item.get('id')}: {name}")
            client.delete("products", int(item["id"]))
            continue
        raise SmokeSetupError(f"Shared store is locked by {name} (product {item.get('id')}).")

    product = client.create(
        "products",
        {
            "name": f"SUITE-LOCK-{run_id}-{int(time.time())}",
            "type": "simple",
            "status": "draft",
            "catalog_visibility": "hidden",
            "regular_price": "0",
            "sku": f"lock-{run_id}",
        },
    )
    lock_record = {"type": "lock_product", "id": int(product["id"]), "label": product["name"]}
    if args.manifest:
        path = Path(args.manifest)
        manifest = read_json(path) if path.exists() else manifest_template(run_id, args.store)
        manifest["lock"] = lock_record
        manifest.setdefault("entities", []).append(lock_record)
        write_json(path, manifest)
    print(json.dumps(lock_record))


def unlock(args: argparse.Namespace) -> None:
    load_store_env(args.store)
    client = WooClient()
    lock_id = args.lock_id
    if not lock_id and args.manifest and Path(args.manifest).exists():
        manifest = read_json(Path(args.manifest))
        lock_data = manifest.get("lock") or {}
        lock_id = lock_data.get("id")
    if not lock_id:
        print("No lock id supplied; nothing to unlock")
        return
    client.delete("products", int(lock_id))
    print(f"Deleted shared-store lock product {lock_id}")


def entity_label(entity_type: str, item: dict[str, Any]) -> str:
    if entity_type == "coupon":
        return str(item.get("code", ""))
    if entity_type == "customer":
        return " ".join(
            str(item.get(key, "")) for key in ("first_name", "last_name", "email", "username")
        )
    if entity_type == "order":
        billing = item.get("billing", {}) or {}
        return " ".join(
            str(value)
            for value in (
                item.get("customer_note", ""),
                billing.get("first_name", ""),
                billing.get("last_name", ""),
                billing.get("email", ""),
            )
        )
    return str(item.get("name", ""))


def write_env_file(path: Path, values: dict[str, str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for key, value in sorted(values.items()):
            escaped = value.replace("\\", "\\\\").replace('"', '\\"')
            handle.write(f'export {key}="{escaped}"\n')


def write_json(path_value: str | Path, value: dict[str, Any]) -> None:
    path = Path(path_value)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    seed_parser = subparsers.add_parser("seed")
    seed_parser.add_argument("--store", choices=("lab", "shared"), required=True)
    seed_parser.add_argument("--run-id", required=True)
    seed_parser.add_argument("--manifest", required=True)
    seed_parser.add_argument("--env-file")
    seed_parser.set_defaults(func=seed)

    cleanup_parser = subparsers.add_parser("cleanup")
    cleanup_parser.add_argument("--manifest", required=True)
    cleanup_parser.add_argument("--store", choices=("lab", "shared"))
    cleanup_parser.set_defaults(func=cleanup)

    sweep_parser = subparsers.add_parser("sweep")
    sweep_parser.add_argument("--store", choices=("lab", "shared"), required=True)
    sweep_parser.add_argument("--report")
    sweep_parser.add_argument("--dry-run", action="store_true")
    sweep_parser.set_defaults(func=sweep)

    lock_parser = subparsers.add_parser("lock")
    lock_parser.add_argument("--store", choices=("shared",), default="shared")
    lock_parser.add_argument("--run-id", required=True)
    lock_parser.add_argument("--manifest")
    lock_parser.add_argument("--ttl-seconds", type=int, default=LOCK_TTL_SECONDS)
    lock_parser.set_defaults(func=lock)

    unlock_parser = subparsers.add_parser("unlock")
    unlock_parser.add_argument("--store", choices=("shared",), default="shared")
    unlock_parser.add_argument("--manifest")
    unlock_parser.add_argument("--lock-id", type=int)
    unlock_parser.set_defaults(func=unlock)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        args.func(args)
        return 0
    except SmokeSetupError as exc:
        print(f"Setup error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
