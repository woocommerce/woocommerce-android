import argparse
import contextlib
import importlib.util
import io
import json
import tempfile
import unittest
import unittest.mock
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "seed-fixtures.py"
SPEC = importlib.util.spec_from_file_location("seed_fixtures", SCRIPT)
assert SPEC and SPEC.loader
seed_fixtures = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(seed_fixtures)


class FailingWooClient:
    def __init__(self) -> None:
        self.create_count = 0

    def create(self, path: str, payload: dict) -> dict:
        self.create_count += 1
        if self.create_count == 2:
            raise seed_fixtures.SmokeSetupError("injected create failure")
        return {"id": 101}


class PartiallyFailingCleanupClient:
    def __init__(self) -> None:
        self.delete_count = 0

    def list(self, path: str, **query: object) -> list[dict[str, object]]:
        return []

    def delete(self, path: str, entity_id: int) -> None:
        self.delete_count += 1
        if self.delete_count == 2:
            raise seed_fixtures.SmokeSetupError("injected cleanup failure")


class RunOwnedStragglerClient:
    """Returns a coupon and a tag the flow made in the UI, absent from the manifest."""

    def __init__(self) -> None:
        self.deleted: list[tuple[str, int]] = []

    def list(self, path: str, **query: object) -> list[dict[str, object]]:
        if path == "coupons":
            return [
                {"id": 900, "code": "suite-20260805-abc123-ui"},
                {"id": 901, "code": "summer-sale"},
            ]
        if path == "products/tags":
            return [
                {"id": 910, "name": "Maestro tag SUITE-20260805-abc123"},
                {"id": 911, "name": "Sale"},
            ]
        return []

    def delete(self, path: str, entity_id: int) -> None:
        self.deleted.append((path, entity_id))


class SeededProductOrderClient:
    """Returns a POS order with a seeded variation and an unrelated order."""

    def __init__(self) -> None:
        self.deleted: list[tuple[str, int]] = []
        self.order_queries: list[dict[str, object]] = []

    def list(self, path: str, **query: object) -> list[dict[str, object]]:
        if path == "orders" and "after" in query:
            self.order_queries.append(query)
            return [
                {"id": 300, "line_items": [{"name": "Large", "product_id": 0, "variation_id": 202}]},
                {"id": 301, "line_items": [{"name": "Album", "product_id": 15, "variation_id": 0}]},
                {"id": 302, "line_items": [{"name": "Small", "product_id": 201, "variation_id": 202}]},
            ]
        return []

    def delete(self, path: str, entity_id: int) -> None:
        self.deleted.append((path, entity_id))


class SeedFixturesTests(unittest.TestCase):
    def test_missing_store_value_names_the_selected_store_variable(self) -> None:
        with unittest.mock.patch.dict(seed_fixtures.os.environ, {}, clear=True):
            seed_fixtures.load_store_env("lab")
            with self.assertRaisesRegex(seed_fixtures.SmokeSetupError, "MAESTRO_WOO_LAB_CONSUMER_KEY"):
                seed_fixtures.env_required("MAESTRO_WOO_CONSUMER_KEY")

    def test_failed_seed_persists_every_entity_created_before_the_failure(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "run-manifest.json"
            args = argparse.Namespace(
                run_id="SUITE-20260805-abc123",
                store="lab",
                manifest=str(manifest),
                env_file=None,
            )
            original_client = seed_fixtures.WooClient
            seed_fixtures.WooClient = FailingWooClient
            try:
                with self.assertRaisesRegex(seed_fixtures.SmokeSetupError, "injected create failure"):
                    seed_fixtures.seed(args)
            finally:
                seed_fixtures.WooClient = original_client

            saved = json.loads(manifest.read_text(encoding="utf-8"))

        self.assertEqual(
            [{"id": 101, "label": "variable product tag", "type": "product_tag"}],
            saved["entities"],
        )

    def test_cleanup_journals_each_successful_deletion_before_continuing(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "run-manifest.json"
            manifest.write_text(
                json.dumps(
                    {
                        "run_id": "SUITE-20260805-abc123",
                        "store": "lab",
                        "entities": [
                            {"type": "product", "id": 301, "label": "first"},
                            {"type": "product", "id": 302, "label": "second"},
                        ],
                    }
                ),
                encoding="utf-8",
            )
            args = argparse.Namespace(manifest=str(manifest), store=None)
            original_client = seed_fixtures.WooClient
            seed_fixtures.WooClient = PartiallyFailingCleanupClient
            try:
                with (
                    contextlib.redirect_stderr(io.StringIO()),
                    self.assertRaisesRegex(seed_fixtures.SmokeSetupError, "1 deletion error"),
                ):
                    seed_fixtures.cleanup(args)
            finally:
                seed_fixtures.WooClient = original_client

            saved = json.loads(manifest.read_text(encoding="utf-8"))

        self.assertEqual(
            [{"type": "product", "id": 301, "label": "first"}],
            saved["entities"],
        )

    def test_cleanup_removes_run_owned_entities_the_flow_created_outside_the_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "run-manifest.json"
            manifest.write_text(
                json.dumps(
                    {"run_id": "SUITE-20260805-abc123", "store": "lab", "entities": []}
                ),
                encoding="utf-8",
            )
            args = argparse.Namespace(manifest=str(manifest), store=None)
            original_client = seed_fixtures.WooClient
            client = RunOwnedStragglerClient()
            seed_fixtures.WooClient = lambda: client
            try:
                with contextlib.redirect_stdout(io.StringIO()):
                    seed_fixtures.cleanup(args)
            finally:
                seed_fixtures.WooClient = original_client

        self.assertEqual([("coupons", 900), ("products/tags", 910)], client.deleted)

    def test_cleanup_deletes_run_orders_that_hold_a_seeded_product(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest_path = Path(directory) / "run-manifest.json"
            manifest_path.write_text(
                json.dumps(
                    {
                        "run_id": "SUITE-20260805-abc123",
                        "store": "lab",
                        "created_at": "2026-08-05T10:00:00+00:00",
                        "entities": [
                            {"type": "product", "id": 201},
                            {"type": "product_variation", "id": 202},
                            {"type": "order", "id": 302},
                        ],
                    }
                ),
                encoding="utf-8",
            )
            client = SeededProductOrderClient()
            original_client = seed_fixtures.WooClient
            seed_fixtures.WooClient = lambda: client
            try:
                with contextlib.redirect_stdout(io.StringIO()):
                    seed_fixtures.cleanup(argparse.Namespace(manifest=str(manifest_path), store="lab"))
            finally:
                seed_fixtures.WooClient = original_client

        self.assertIn(("orders", 300), client.deleted)
        self.assertNotIn(("orders", 301), client.deleted)
        self.assertEqual(1, client.deleted.count(("orders", 302)))
        self.assertEqual("2026-08-05T10:00:00", client.order_queries[0]["after"])
        self.assertEqual(("orders", 300), client.deleted[0])


if __name__ == "__main__":
    unittest.main()
