"""Generate release Markdown from Conventional Commits, without GitHub PRs."""

import argparse
from pathlib import Path
import re
import subprocess


VERSION_TAG = re.compile(
    r"v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?"
)
CONVENTIONAL = re.compile(r"([a-zA-Z]+)(?:\(([^()]+)\))?(!)?:\s+(.+)")
SECTIONS = ("Breaking changes", "Features", "Fixes", "Performance", "Other changes")
INTERNAL_TYPES = {"build", "chore", "refactor", "style", "test"}


def git(*args):
    return subprocess.check_output(["git", *args], text=True, encoding="utf-8").strip()


def version_key(tag):
    match = VERSION_TAG.fullmatch(tag)
    if not match:
        raise ValueError(f"Invalid version tag: {tag}")
    prerelease = match[4]
    identifiers = tuple(
        (0, int(part)) if part.isdigit() else (1, part)
        for part in (prerelease or "").split(".")
    )
    return (*map(int, match.group(1, 2, 3)), prerelease is None, identifiers)


def previous_tag(tag):
    current = version_key(tag)
    candidates = []
    for candidate in git("tag", "--merged", f"refs/tags/{tag}").splitlines():
        if not VERSION_TAG.fullmatch(candidate):
            continue
        key = version_key(candidate)
        # Stable releases include everything since the last stable release,
        # even if some changes already appeared in a release candidate.
        if key < current and (not current[3] or key[3]):
            candidates.append(candidate)
    return max(candidates, key=version_key, default=None)


def escape_markdown(value):
    return re.sub(r"([\\`*_{}\[\]<>()])", r"\\\1", value)


def release_notes(tag, repository):
    version_key(tag)
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Repository must be owner/name")
    if git("rev-parse", "--is-shallow-repository") == "true":
        raise ValueError("Release notes require full history; use checkout fetch-depth: 0")
    git("rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}")
    previous = previous_tag(tag)
    revision = f"refs/tags/{previous}..refs/tags/{tag}" if previous else f"refs/tags/{tag}"
    fields = git("log", "--no-merges", "--reverse", "-z", "--format=%H%x00%s%x00%b", revision).split("\0")
    groups = {section: [] for section in SECTIONS}
    url = f"https://github.com/{repository}"

    for index in range(0, len(fields) - 1, 3):
        sha, subject, body = fields[index:index + 3]
        match = CONVENTIONAL.fullmatch(subject)
        footer = re.search(r"^BREAKING[ -]CHANGE:\s*(\S[^\n]*)", body, re.MULTILINE)
        kind, scope, bang, description = match.groups() if match else (None, None, None, subject)
        kind = kind.lower() if kind else None
        if kind in {"docs", "ci"}:
            continue
        breaking = bool(bang or footer)
        if kind in INTERNAL_TYPES and not breaking:
            continue
        section = "Breaking changes" if breaking else {
            "feat": "Features", "fix": "Fixes", "perf": "Performance"
        }.get(kind, "Other changes")
        text = f"{scope}: {description}" if scope else description
        if footer:
            text += f" — {footer[1]}"
        groups[section].append(f"- {escape_markdown(text)} ([{sha[:7]}]({url}/commit/{sha}))")

    lines = []
    for section, entries in groups.items():
        if entries:
            lines.extend([f"## {section}", "", *entries, ""])
    if not lines:
        lines.extend(["No user-facing changes in this release.", ""])
    changelog = f"{url}/compare/{previous}...{tag}" if previous else f"{url}/commits/{tag}"
    lines.append(f"**Full changelog:** {changelog}")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--repository", required=True, help="GitHub owner/name")
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        notes = release_notes(args.tag, args.repository)
    except (ValueError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"Release notes generation failed: {error}\n")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(notes, encoding="utf-8")


if __name__ == "__main__":
    main()
