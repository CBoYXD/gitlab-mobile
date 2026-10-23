import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "generate-release-notes.py"
SPEC = importlib.util.spec_from_file_location("release_notes", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.previous_cwd = Path.cwd()
        os.chdir(self.temp.name)
        self.addCleanup(os.chdir, self.previous_cwd)
        self.git("init", "-q", "-b", "main")
        self.git("config", "user.name", "Release test")
        self.git("config", "user.email", "release@example.test")
        self.git("config", "commit.gpgsign", "false")
        self.git("config", "tag.gpgsign", "false")
        self.commit("chore: initial commit")

    def git(self, *args):
        return subprocess.check_output(["git", *args], text=True).strip()

    def commit(self, subject, body=None):
        args = ["commit", "-q", "--allow-empty", "-m", subject]
        if body:
            args.extend(["-m", body])
        self.git(*args)
        return self.git("rev-parse", "HEAD")

    def notes(self, tag="v1.1.0"):
        self.git("tag", tag)
        return MODULE.release_notes(tag, "example/mobile")

    def test_categories_scopes_links_and_internal_changes(self):
        self.git("tag", "v1.0.0")
        feature = self.commit("feat(accounts): switch accounts")
        self.commit("fix: correct comment counts")
        self.commit("perf: speed up loading")
        for kind in ["docs", "ci", "chore", "test", "build", "style", "refactor"]:
            self.commit(f"{kind}: internal-only {kind}")
        notes = self.notes()
        self.assertIn("## Features\n\n- accounts: switch accounts", notes)
        self.assertIn("## Fixes\n\n- correct comment counts", notes)
        self.assertIn("## Performance\n\n- speed up loading", notes)
        self.assertIn(f"[{feature[:7]}](https://github.com/example/mobile/commit/{feature})", notes)
        self.assertNotIn("internal-only", notes)
        self.assertIn("/compare/v1.0.0...v1.1.0", notes)

    def test_untyped_changes_are_preserved_and_markdown_is_escaped(self):
        self.commit("Support [self-hosted] <GitLab> *servers*")
        notes = self.notes()
        self.assertIn("## Other changes", notes)
        self.assertIn(r"Support \[self-hosted\] \<GitLab\> \*servers\*", notes)

    def test_breaking_changes_override_internal_type_and_support_footers(self):
        self.commit("feat(auth)!: replace token format")
        self.commit("refactor(api): change pagination", "BREAKING CHANGE: use page cursors")
        self.commit("fix: change account storage", "BREAKING-CHANGE: sign in again")
        self.commit("docs!: hidden docs change")
        notes = self.notes()
        self.assertIn("## Breaking changes", notes)
        self.assertIn("auth: replace token format", notes)
        self.assertIn("change pagination — use page cursors", notes)
        self.assertIn("change account storage — sign in again", notes)
        self.assertNotIn("hidden docs change", notes)

    def test_stable_release_includes_changes_previously_in_prereleases(self):
        self.git("tag", "v1.0.0")
        self.commit("feat: feature in release candidate")
        self.git("tag", "v1.1.0-rc.1")
        self.commit("fix: final correction")
        notes = self.notes()
        self.assertIn("feature in release candidate", notes)
        self.assertIn("final correction", notes)
        self.assertIn("/compare/v1.0.0...v1.1.0", notes)

    def test_prerelease_uses_previous_prerelease_with_numeric_order(self):
        self.git("tag", "v1.0.0")
        self.commit("feat: in rc2")
        self.git("tag", "v1.1.0-rc.2")
        self.commit("fix: in rc10")
        self.git("tag", "v1.1.0-rc.10")
        self.commit("fix: in rc11")
        notes = self.notes("v1.1.0-rc.11")
        self.assertNotIn("in rc2", notes)
        self.assertNotIn("in rc10", notes)
        self.assertIn("in rc11", notes)
        self.assertIn("/compare/v1.1.0-rc.10...v1.1.0-rc.11", notes)

    def test_first_release_includes_history_and_handles_annotated_tag(self):
        self.commit("feat: first feature")
        self.git("tag", "-a", "v1.0.0", "-m", "First release")
        notes = MODULE.release_notes("v1.0.0", "example/mobile")
        self.assertIn("first feature", notes)
        self.assertIn("/commits/v1.0.0", notes)
        self.assertNotIn("/compare/", notes)

    def test_internal_only_release_has_honest_fallback(self):
        self.git("tag", "v1.0.0")
        self.commit("ci: change pipeline")
        self.commit("docs: update guide")
        notes = self.notes()
        self.assertIn("No user-facing changes", notes)
        self.assertNotIn("change pipeline", notes)
        self.assertIn("Full changelog", notes)

    def test_tagged_commit_is_used_instead_of_checkout_head(self):
        self.git("tag", "v1.0.0")
        self.commit("fix: shipped correction")
        self.git("tag", "v1.1.0")
        self.commit("feat: unreleased feature")
        notes = MODULE.release_notes("v1.1.0", "example/mobile")
        self.assertIn("shipped correction", notes)
        self.assertNotIn("unreleased feature", notes)

    def test_unmerged_and_higher_version_tags_are_not_release_baselines(self):
        self.git("tag", "v1.0.0")
        self.git("switch", "-q", "-c", "other")
        self.commit("feat: unrelated work")
        self.git("tag", "v1.0.9")
        self.git("switch", "-q", "main")
        self.commit("fix: included correction")
        self.git("tag", "v9.0.0")
        notes = self.notes()
        self.assertIn("included correction", notes)
        self.assertNotIn("unrelated work", notes)
        self.assertIn("/compare/v1.0.0...v1.1.0", notes)

    def test_merged_branch_changes_are_included_without_merge_messages(self):
        self.git("tag", "v1.0.0")
        self.git("switch", "-q", "-c", "feature")
        self.commit("feat: merged feature")
        self.git("switch", "-q", "main")
        self.git("merge", "-q", "--no-ff", "feature", "-m", "Merge feature branch")
        notes = self.notes()
        self.assertIn("merged feature", notes)
        self.assertNotIn("Merge feature branch", notes)

    def test_cli_writes_utf8_output_and_creates_parent_directory(self):
        self.commit("feat: улучшить поиск")
        self.git("tag", "v1.0.0")
        output = Path("dist/release-notes.md")
        subprocess.run([
            "python3", str(SCRIPT), "--tag", "v1.0.0",
            "--repository", "example/mobile", "--output", str(output),
        ], check=True)
        self.assertIn("улучшить поиск", output.read_text(encoding="utf-8"))

    def test_invalid_tag_and_repository_are_rejected(self):
        for tag, repository in [("main", "example/mobile"), ("v1.0.0", "../invalid")]:
            with self.subTest(tag=tag, repository=repository), self.assertRaises(ValueError):
                MODULE.release_notes(tag, repository)

    def test_shallow_history_is_rejected(self):
        self.commit("feat: feature")
        self.git("tag", "v1.0.0")
        clone = Path(self.temp.name, "shallow")
        self.git("clone", "-q", "--depth=1", Path(self.temp.name).as_uri(), str(clone))
        os.chdir(clone)
        with self.assertRaisesRegex(ValueError, "full history"):
            MODULE.release_notes("v1.0.0", "example/mobile")


if __name__ == "__main__":
    unittest.main()
