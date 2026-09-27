#!/usr/bin/env python3
"""Refreshes the file and folder icons in app/src/main/assets/file-icons.

The icons come from Material Icon Theme (https://github.com/material-extensions/vscode-material-icon-theme,
MIT). This downloads the npm package and keeps only the popular icons listed below (and their
light-theme variants): the lookup tables (file names, extensions, folder names) are cut down to
entries that point at one of them, and only those SVGs are copied. Anything else falls back to the
plain file or folder icon. Add a name to POPULAR_* to ship another icon.

Usage: tools/update_file_icons.py [version]
"""
import io
import json
import pathlib
import sys
import tarfile
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/file-icons"

POPULAR_FILES = """
file python python-misc typescript typescript-def javascript react react_ts kotlin java rust go go-mod
c cpp h hpp csharp swift dart ruby php lua scala vue svelte angular html css sass markdown json yaml
toml xml svg image pdf document log lock database console powershell docker git gitlab nodejs npm
yarn pnpm tsconfig eslint prettier babel webpack vite jest vitest test-ts test-js test-jsx gradle
maven cmake makefile settings tune editorconfig readme changelog license key certificate http
graphql proto terraform nix kubernetes nginx jupyter font audio video zip table diff tailwindcss
next android
""".split()

POPULAR_FOLDERS = """
folder folder-src folder-test folder-docs folder-github folder-gitlab folder-node folder-lib
folder-dist folder-config folder-scripts folder-images folder-public folder-components folder-app
folder-api folder-utils folder-resource folder-tools folder-vscode folder-intellij folder-ci
folder-docker folder-kubernetes folder-database folder-migrations folder-css folder-i18n
folder-hook folder-server folder-client folder-include folder-core folder-views folder-routes
folder-plugin folder-mock folder-temp folder-log folder-secure folder-target folder-java
folder-python folder-rust folder-go folder-kotlin folder-javascript folder-typescript
folder-android folder-ios folder-git folder-examples folder-packages folder-ui folder-coverage
folder-gradle
""".split()


def main() -> None:
    version = sys.argv[1] if len(sys.argv) > 1 else json.load(
        urllib.request.urlopen("https://registry.npmjs.org/material-icon-theme/latest"))["version"]
    url = f"https://registry.npmjs.org/material-icon-theme/-/material-icon-theme-{version}.tgz"
    archive = tarfile.open(fileobj=io.BytesIO(urllib.request.urlopen(url).read()))

    def read(name: str) -> bytes:
        return archive.extractfile(f"package/{name}").read()

    manifest = json.loads(read("dist/material-icons.json"))
    light = manifest.get("light", {})

    def lower(table: dict) -> dict:
        return {k.lower(): v for k, v in table.items()}

    tables = {
        "version": version,
        "file": manifest["file"],
        "folder": manifest["folder"],
        "fileNames": lower(manifest["fileNames"]),
        "fileExtensions": lower(manifest["fileExtensions"]),
        "folderNames": lower(manifest["folderNames"]),
        "light": {
            "fileNames": lower(light.get("fileNames", {})),
            "fileExtensions": lower(light.get("fileExtensions", {})),
            "folderNames": lower(light.get("folderNames", {})),
        },
    }
    definitions = manifest["iconDefinitions"]
    popular = [name for name in POPULAR_FILES + POPULAR_FOLDERS if name in definitions]
    missing = sorted(set(POPULAR_FILES + POPULAR_FOLDERS) - set(popular))
    if missing:
        print("not in this version, skipped:", " ".join(missing))
    allowed = set(popular) | {f"{name}_light" for name in popular if f"{name}_light" in definitions}
    for table in (tables, tables["light"]):
        for key in ("fileNames", "fileExtensions", "folderNames"):
            table[key] = {k: v for k, v in table[key].items() if v in allowed}
    used = {tables["file"], tables["folder"]}
    for key in ("fileNames", "fileExtensions", "folderNames"):
        used.update(tables[key].values())
        used.update(tables["light"][key].values())

    if OUT.exists():
        for old in OUT.glob("*.svg"):
            old.unlink()
    OUT.mkdir(parents=True, exist_ok=True)
    for icon in sorted(used):
        path = definitions[icon]["iconPath"].split("/icons/")[-1]
        (OUT / f"{icon}.svg").write_bytes(read(f"icons/{path}"))
    (OUT / "icons.json").write_text(json.dumps(tables, separators=(",", ":"), sort_keys=True))
    (OUT / "LICENSE").write_bytes(read("LICENSE"))
    print(f"material-icon-theme {version}: {len(used)} icons")


if __name__ == "__main__":
    main()
