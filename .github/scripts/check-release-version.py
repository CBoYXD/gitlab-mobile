"""Reject branch releases, malformed tags and tags with the wrong app version."""

import os
from pathlib import Path
import re
import sys


def main():
    ref = os.environ.get("GITHUB_REF", "")
    match = re.fullmatch(
        r"refs/tags/v((?:0|[1-9]\d*)\.(?:0|[1-9]\d*)\.(?:0|[1-9]\d*)"
        r"(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?)",
        ref,
    )
    if not match:
        sys.exit("::error::Release requires a tag such as v1.5.0 or v1.5.0-rc.1, not a branch.")
    version = match[1]
    build_file = Path("app/build.gradle.kts").read_text()
    app_version = re.search(r'^\s*versionName\s*=\s*"([^"]+)"', build_file, re.MULTILINE)
    if app_version is None or app_version[1] != version:
        sys.exit(f"::error::Tag version {version} must match versionName in app/build.gradle.kts.")
    print(f"Release version: {version}")
    if output := os.environ.get("GITHUB_OUTPUT"):
        with open(output, "a") as stream:
            stream.write(f"version={version}\n")


if __name__ == "__main__":
    main()
