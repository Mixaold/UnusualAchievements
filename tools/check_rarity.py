"""Checks the rarity pairing without starting the game.

RarityBasis.verifyCoverage() enforces the same invariant at server start, but only once the game is
actually running. This reads the sources instead, so the pairing can be checked from a terminal
right after a rename: every registered achievement has a basis, every basis points at an achievement
that exists, no achievement is claimed by two bases, and every basis has its sentence in both lang
files.

    python tools/check_rarity.py

Exit code 0 means clean, 1 lists what drifted. Run from the project root.
"""
import io
import json
import re
import sys

DEFS = "src/main/java/dev/semisaint/unusualachievements/fabric/registry/AchievementDefinitions.java"
BASIS = "src/main/java/dev/semisaint/unusualachievements/fabric/registry/RarityBasis.java"
LANG = "src/main/resources/assets/unusualachievements/lang/%s.json"


def read(path):
    return io.open(path, encoding="utf-8").read()


def main():
    defs, basis = read(DEFS), read(BASIS)
    problems = []

    # Constant -> id string, then which constants actually reach the registry. A constant that is
    # declared but never registered is not an achievement, and must not be expected to have a basis.
    const_to_path = dict(re.findall(r'AchievementId\s+(\w+)\s*=\s*new AchievementId\("([^"]+)"\)', defs))
    registered_consts = re.findall(r'AchievementRegistry\.register\(new AchievementDefinition\(\s*(\w+)\s*,', defs)
    registered = [const_to_path[c] for c in registered_consts if c in const_to_path]

    unknown = [c for c in registered_consts if c not in const_to_path]
    if unknown:
        problems.append("registered under a constant with no AchievementId: %s" % unknown)
    dupes = sorted({p for p in registered if registered.count(p) > 1})
    if dupes:
        problems.append("registered more than once: %s" % dupes)

    # map(BASIS, "a", "b", ...) - the achievement side is a plain string, which is the part that rots.
    mapped = {}
    for m in re.finditer(r'map\((\w+),\s*((?:"[^"]+"\s*,?\s*)+)\)', basis, re.S):
        for path in re.findall(r'"([^"]+)"', m.group(2)):
            mapped.setdefault(path, []).append(m.group(1))
    multi = {p: b for p, b in mapped.items() if len(b) > 1}
    if multi:
        problems.append("claimed by two bases: %s" % multi)

    registered_set, mapped_set = set(registered), set(mapped)
    no_basis = sorted(registered_set - mapped_set)
    dead = sorted(mapped_set - registered_set)
    if no_basis:
        problems.append("registered but no rarity basis (line would silently vanish): %s" % no_basis)
    if dead:
        problems.append("basis points at an achievement that does not exist: %s" % dead)

    # Every enum constant needs its whole sentence in every language.
    enum_keys = sorted(set(re.findall(r'\w+\(Stats\.\w+,\s*"(\w+)"', basis)))
    for lang in ("en_us", "ru_ru"):
        strings = json.load(io.open(LANG % lang, encoding="utf-8"))
        for key in enum_keys + ["percent", "among_players"]:
            if "rarity.unusualachievements." + key not in strings:
                problems.append("%s: no line for '%s'" % (lang, key))

    print("achievements registered : %d" % len(registered_set))
    print("paired with a basis     : %d" % len(mapped_set))
    print("bases in the enum       : %d" % len(enum_keys))

    if problems:
        print("\nBROKEN:")
        for problem in problems:
            print("  -", problem)
        return 1
    print("\nclean - every achievement is paired, every basis is real, both languages complete")
    return 0


if __name__ == "__main__":
    sys.exit(main())
