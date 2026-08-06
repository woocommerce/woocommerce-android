import argparse
import contextlib
import importlib.util
import io
import json
import tempfile
import unittest
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


class LockWooClient:
    def list(self, path: str, **query) -> list[dict]:
        return []

    def create(self, path: str, payload: dict) -> dict:
        return {"id": 202, "name": payload["name"]}


class PartiallyFailingCleanupClient:
    def __init__(self) -> None:
        self.delete_count = 0

    def delete(self, path: str, entity_id: int) -> None:
        self.delete_count += 1
        if self.delete_count == 2:
            raise seed_fixtures.SmokeSetupError("injected cleanup failure")


class SeedFixturesTests(unittest.TestCase):
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

    def test_lock_lifecycle_is_separate_from_fixture_entities(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "run-manifest.json"
            args = argparse.Namespace(
                run_id="SUITE-20260805-abc123",
                store="shared",
                manifest=str(manifest),
                ttl_seconds=60,
            )
            original_client = seed_fixtures.WooClient
            seed_fixtures.WooClient = LockWooClient
            try:
                with contextlib.redirect_stdout(io.StringIO()):
                    seed_fixtures.lock(args)
            finally:
                seed_fixtures.WooClient = original_client

            saved = json.loads(manifest.read_text(encoding="utf-8"))

        self.assertEqual(202, saved["lock"]["id"])
        self.assertEqual([], saved["entities"])

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


if __name__ == "__main__":
    unittest.main()
