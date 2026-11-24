#!/usr/bin/env python3
"""Graft recompiled classes onto a disassembled base dex, keeping synthetic helpers consistent.

D8 numbers the `$$ExternalSyntheticBackport` and `$$ExternalSyntheticRecord` helper classes
per compilation, so `Backport0` from one build means nothing in the next. Copying our
classes by *name* therefore leaves the untouched base classes pointing at a helper whose
signature moved: the app dies at startup with NoSuchMethodError, inside whatever code
happens to initialise first, not inside our own.

Synthetic helpers are matched by the set of method signatures they declare instead, and are
kept out of the per-class copy.

usage: splice-classes.py --base <disassembled-base> --new <disassembled-new> \
                         --out <disassembled-out> --prefix LocalDownloadManager --prefix TopTracks
"""
import argparse
import os
import re
import shutil
import sys

CLASS_RE = re.compile(r'^\.class[^\n]*?L([^;\n]+);', re.M)
METHOD_RE = re.compile(r'^\.method[^\n]*?([A-Za-z0-9_$<>]+)\((.*?)\)(\S+)\s*$', re.M)
SYNTHETIC = '$$ExternalSynthetic'


def read(path):
    with open(path, errors='ignore') as f:
        return f.read()


def describe(path):
    """(class descriptor, frozenset of method signatures) for one smali file."""
    txt = read(path)
    m = CLASS_RE.search(txt)
    if not m:
        return None, frozenset()
    return m.group(1), frozenset(METHOD_RE.findall(txt))


def index(root):
    """Map class descriptor -> (path, signatures) for every smali file under root."""
    out = {}
    for dirpath, _, files in os.walk(root):
        for f in files:
            if not f.endswith('.smali'):
                continue
            path = os.path.join(dirpath, f)
            desc, sigs = describe(path)
            if desc:
                out[desc] = (path, sigs)
    return out


def is_synthetic(desc):
    return SYNTHETIC in desc


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--base', required=True, help='disassembled pristine dex')
    ap.add_argument('--new', required=True, help='disassembled recompiled classes')
    ap.add_argument('--out', required=True, help='disassembled tree to produce')
    ap.add_argument('--prefix', action='append', required=True,
                    help='outer class simple name whose classes are replaced')
    ap.add_argument('--suffix', default='Morphe',
                    help='marker appended to renamed helpers, to keep them unique')
    args = ap.parse_args()

    base = index(args.base)
    new = index(args.new)

    def owned(desc):
        simple = desc.rsplit('/', 1)[-1]
        return any(simple == p or simple.startswith(p + '$') or
                   simple.startswith(p + '$$') for p in args.prefix)

    # D8 numbers the synthetic helpers per compilation, so a name from our build means
    # something else in the base. Every helper we bring in therefore gets a name that cannot
    # exist in the base, and every reference to it is rewritten, including the ones inside
    # the helpers themselves -- two helpers in the same build call each other, and renaming
    # only the owning class turned that into a self-call on a signature it does not have.
    suffix = '$' + args.suffix
    rename = {}
    for desc in new:
        if owned(desc) and is_synthetic(desc):
            candidate = desc + suffix
            if candidate in base or candidate in new:
                print(f"refusing to build {candidate}: name already taken", file=sys.stderr)
                return 1
            rename[desc] = candidate

    if os.path.exists(args.out):
        shutil.rmtree(args.out)
    shutil.copytree(args.base, args.out)

    # Two kinds of helper share our subtree and must be treated differently.
    #
    # Backport and Record helpers are shared services: D8 deduplicates them, so
    # StreamCandidates$$ExternalSyntheticBackport0 ends up called from 33 unrelated classes
    # across the extension, and SpoofVideoStreamsPatch calls it from its static initialiser.
    # Deleting one to replace it took the app down at launch. They stay, and ours live under
    # fresh names beside them.
    #
    # Lambda helpers are the opposite: base LocalDownloadManager instantiates them, so with
    # the class replaced they are orphaned, and their bodies reference lambda methods that no
    # longer exist. They go, or the dex carries dangling references.
    removed = 0
    for desc, (path, _) in base.items():
        if not owned(desc):
            continue
        if is_synthetic(desc) and 'ExternalSyntheticLambda' not in desc:
            continue
        target = os.path.join(args.out, os.path.relpath(path, args.base))
        if os.path.exists(target):
            os.remove(target)
            removed += 1

    def rewrite(text):
        # Longest first: a helper name is a prefix of nothing, but the bare class name is a
        # prefix of its own synthetics, and partial replacement would corrupt them.
        for old in sorted(rename, key=len, reverse=True):
            text = text.replace('L%s;' % old, 'L%s;' % rename[old])
        return text

    copied = 0
    for desc, (path, _) in new.items():
        if not owned(desc):
            continue
        target_desc = rename.get(desc, desc)
        rel = target_desc.replace('.', '/') + '.smali'
        target = os.path.join(args.out, rel)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, 'w') as f:
            f.write(rewrite(read(path)))
        copied += 1

    print(f"base classes   : {len(base)}")
    print(f"removed (owned): {removed}")
    print(f"copied         : {copied}")
    print(f"helpers renamed: {len(rename)}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
