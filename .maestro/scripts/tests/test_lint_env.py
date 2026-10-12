from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "lint-env.py"
SPEC = importlib.util.spec_from_file_location("lint_env", SCRIPT)
assert SPEC and SPEC.loader
LINT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(LINT)


class LintEnvTests(unittest.TestCase):
    def lint(self, contents: str) -> list[str]:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            env = root / ".env.local"
            example = root / "env.example"
            env.write_text(contents)
            example.write_text("MAESTRO_WOO_LAB_WPCOM_PASSWORD=\n")
            errors, _, _ = LINT.lint(env, example, False)
        return errors

    def test_rejects_values_that_bash_changes_when_sourcing_the_file(self) -> None:
        self.assertTrue(self.lint('MAESTRO_WOO_LAB_WPCOM_PASSWORD="ab$$cd"\n'))
        self.assertTrue(self.lint("MAESTRO_WOO_LAB_WPCOM_PASSWORD=ab\\cd\n"))

    def test_accepts_single_quoted_shell_metacharacters(self) -> None:
        self.assertEqual([], self.lint("MAESTRO_WOO_LAB_WPCOM_PASSWORD='ab$$cd\\ef'\n"))


if __name__ == "__main__":
    unittest.main()
