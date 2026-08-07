#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Downloads the 3D icon pack for DarsHub.

Source : https://github.com/microsoft/fluentui-emoji  (MIT, Microsoft)
Why    : it is the only large 3D emoji set with a licence that actually allows
         shipping inside a commercial app. Apple's set is not redistributable.

What it does
  1. scans app/src/main/java for every emoji the app really uses,
  2. downloads the Fluent Emoji repository once,
  3. copies only the matching 3D renders into res/drawable-nodpi,
     named ic3d_<codepoints>.png so core/Icon3D.kt finds them at runtime.

Run it from the project root, with a network connection:

    python3 tools/fetch_3d_icons.py

Nothing else in the project has to change. Icon3D falls back to the plain
emoji for anything missing, so a partial download is harmless.

Heads up: the archive is a few hundred megabytes because the repository also
carries the svg, flat and high contrast variants. It is downloaded to a temp
directory and deleted afterwards. Only the pngs you need are kept.
"""

import io
import json
import os
import re
import shutil
import sys
import tarfile
import tempfile
import urllib.request

TARBALL = "https://github.com/microsoft/fluentui-emoji/archive/refs/heads/main.tar.gz"
SRC_DIR = os.path.join("app", "src", "main", "java")
OUT_DIR = os.path.join("app", "src", "main", "res", "drawable-nodpi")
SKIP = {0xFE0F, 0x200D}


def is_emoji(cp):
    return (
        0x1F300 <= cp <= 0x1FAFF
        or 0x2600 <= cp <= 0x27BF
        or 0x1F000 <= cp <= 0x1F2FF
        or cp in (0x2B50, 0x2B55, 0x2764, 0x2705, 0x274C)
    )


def key_of(codepoints):
    """Same normalisation core/Icon3D.kt uses."""
    return "_".join("%x" % c for c in codepoints if c not in SKIP)


def used_emoji():
    """Every distinct emoji character in the Kotlin sources."""
    found = set()
    for root, _dirs, files in os.walk(SRC_DIR):
        for name in files:
            if not name.endswith(".kt"):
                continue
            with io.open(os.path.join(root, name), encoding="utf-8") as fh:
                for ch in fh.read():
                    if is_emoji(ord(ch)):
                        found.add(ch)
    return found


def main():
    if not os.path.isdir(SRC_DIR):
        sys.exit("run this from the project root (the folder holding app/)")

    wanted = {key_of([ord(c) for c in e]): e for e in used_emoji()}
    print("emoji used by the app : %d" % len(wanted))

    tmp = tempfile.mkdtemp(prefix="fluent-")
    archive = os.path.join(tmp, "fluent.tar.gz")
    try:
        print("downloading Fluent Emoji, this takes a while ...")
        urllib.request.urlretrieve(TARBALL, archive)

        print("extracting ...")
        with tarfile.open(archive) as tf:
            members = [
                m for m in tf.getmembers()
                if "/assets/" in m.name
                and (m.name.endswith("metadata.json") or "/3D/" in m.name)
            ]
            tf.extractall(tmp, members=members)

        root = next(
            os.path.join(tmp, d) for d in os.listdir(tmp)
            if d.startswith("fluentui-emoji")
        )
        assets = os.path.join(root, "assets")

        if not os.path.isdir(OUT_DIR):
            os.makedirs(OUT_DIR)

        copied, missing = 0, []
        index = {}
        for folder in os.listdir(assets):
            meta = os.path.join(assets, folder, "metadata.json")
            if not os.path.isfile(meta):
                continue
            with io.open(meta, encoding="utf-8") as fh:
                data = json.load(fh)
            raw = str(data.get("unicode", ""))
            cps = [int(p, 16) for p in raw.split() if re.fullmatch(r"[0-9a-fA-F]+", p)]
            if cps:
                index.setdefault(key_of(cps), os.path.join(assets, folder))

        for key, emoji in sorted(wanted.items()):
            folder = index.get(key)
            png = None
            if folder:
                # Default/3D wins over the skin tone variants.
                for base, _dirs, files in os.walk(folder):
                    if os.path.basename(base) != "3D":
                        continue
                    for f in files:
                        if f.endswith(".png"):
                            candidate = os.path.join(base, f)
                            if png is None or "Default" in candidate:
                                png = candidate
            if png is None:
                missing.append(emoji)
                continue
            shutil.copyfile(png, os.path.join(OUT_DIR, "ic3d_%s.png" % key))
            copied += 1

        print("\ncopied  : %d icons into %s" % (copied, OUT_DIR))
        if missing:
            print("missing : %s" % "".join(missing))
            print("          (these keep using the normal emoji, nothing breaks)")
        total = sum(
            os.path.getsize(os.path.join(OUT_DIR, f))
            for f in os.listdir(OUT_DIR) if f.startswith("ic3d_")
        )
        print("apk growth : about %.1f MB" % (total / 1024.0 / 1024.0))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
