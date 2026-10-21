#!/usr/bin/env python3
"""Fail when the third-party inventory does not cover what the build declares.

Checks (stdlib only, Python 3.11+):
  * every library and plugin in gradle/libs.versions.toml has a matching component in
    third_party.json (matched on `coordinates`) at the same version;
  * every component has the fields the compliance protocol requires, a known scope,
    and a license that is permissive (or a weak-copyleft license in test/build scope
    only, which is never shipped);
  * every component and "informed by" project is named in THIRD_PARTY_NOTICES.md;
  * the committed Gradle wrapper jar still has the checksum recorded for it, and
    gradle-wrapper.properties pins the distribution checksum;
  * with --resolved FILE (the `group:name:version` lines written by
    `./gradlew printRuntimeDependencies`), every module that actually ships in the
    APK, transitive ones included, is in third_party.json at the resolved version.

It reads files only. Exit 0 = covered, 1 = something is missing or inconsistent.
"""
import hashlib
import json
import argparse
import os
import re
import sys
import tomllib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
REQUIRED = [
    "name", "upstream_url", "version_or_commit", "spdx", "license_verified_from",
    "files", "notices_required", "modified", "scope",
]
SCOPES = {"runtime", "test", "build"}
PERMISSIVE = {"MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC"}
# Weak copyleft is acceptable only when the component is never part of the shipped APK.
NON_SHIPPED_ONLY = {"EPL-1.0", "EPL-2.0"}


def load_json(name):
    with open(os.path.join(ROOT, name), encoding="utf-8") as handle:
        return json.load(handle)


def declared_from_toml():
    """Return (label, coordinate, version) for every library and plugin in the catalog."""
    with open(os.path.join(ROOT, "gradle", "libs.versions.toml"), "rb") as handle:
        toml = tomllib.load(handle)
    versions = toml.get("versions", {})

    def resolve(version):
        if isinstance(version, dict):
            if "ref" in version:
                return versions.get(version["ref"])
            return version.get("strictly") or version.get("require")
        return version

    declared = []
    for alias, spec in toml.get("libraries", {}).items():
        if isinstance(spec, str):
            coordinate, _, version = spec.rpartition(":")
            coordinate = coordinate
        else:
            coordinate = spec.get("module") or "%s:%s" % (spec["group"], spec["name"])
            version = resolve(spec.get("version"))
        declared.append(("library:" + alias, coordinate, version))
    for alias, spec in toml.get("plugins", {}).items():
        declared.append(("plugin:" + alias, spec["id"], resolve(spec.get("version"))))
    return declared


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--resolved", metavar="FILE", help="resolved runtime modules, one group:name:version per line")
    args = parser.parse_args()
    errors = []
    try:
        inventory = load_json("third_party.json")
    except (OSError, ValueError) as exc:
        print("FAIL third_party.json: %s" % exc)
        return 1
    components = inventory.get("components", [])
    informed_by = inventory.get("informed_by", [])

    by_coordinate = {}
    names = set()
    for index, comp in enumerate(components):
        label = comp.get("name", "components[%d]" % index)
        if label in names:
            errors.append("duplicate component name: %s" % label)
        names.add(label)
        for field in REQUIRED:
            if field not in comp or comp[field] in (None, "", []) and field not in ("modified", "notices_required"):
                errors.append("%s: missing field %r" % (label, field))
        if comp.get("scope") not in SCOPES:
            errors.append("%s: scope must be one of %s" % (label, sorted(SCOPES)))
        spdx = comp.get("spdx", "")
        if spdx not in PERMISSIVE:
            if spdx in NON_SHIPPED_ONLY and comp.get("scope") in ("test", "build"):
                pass
            else:
                errors.append(
                    "%s: license %r is not on the permissive list; owner decision required "
                    "before this component is used (%s scope)" % (label, spdx, comp.get("scope"))
                )
        for url in comp.get("license_verified_from", []):
            if not re.match(r"^https?://", url):
                errors.append("%s: license_verified_from entry is not a URL: %r" % (label, url))
        for coordinate in comp.get("coordinates", []):
            by_coordinate.setdefault(coordinate, []).append(comp)

    checked = 0
    for alias, coordinate, version in declared_from_toml():
        checked += 1
        matches = by_coordinate.get(coordinate)
        if not matches:
            errors.append("%s (%s %s) is declared in libs.versions.toml but missing from third_party.json"
                          % (alias, coordinate, version))
            continue
        if not any(str(m.get("version_or_commit")) == str(version) for m in matches):
            errors.append("%s: libs.versions.toml pins %s but third_party.json records %s"
                          % (coordinate, version, [m.get("version_or_commit") for m in matches]))

    resolved_count = None
    if args.resolved:
        resolved_count = 0
        try:
            with open(args.resolved, encoding="utf-8") as handle:
                lines = [line.strip() for line in handle if line.strip()]
        except OSError as exc:
            lines = []
            errors.append("cannot read resolved dependency list: %s" % exc)
        for line in lines:
            resolved_count += 1
            coordinate, _, version = line.rpartition(":")
            matches = [c for c in by_coordinate.get(coordinate, []) if c.get("scope") == "runtime"]
            if not matches:
                errors.append("shipped runtime module %s is not in third_party.json (scope runtime)" % line)
            elif not any(str(c.get("version_or_commit")) == version for c in matches):
                errors.append("shipped runtime module %s resolves to %s but third_party.json records %s"
                              % (coordinate, version, [c.get("version_or_commit") for c in matches]))

    notices_path = os.path.join(ROOT, "THIRD_PARTY_NOTICES.md")
    try:
        with open(notices_path, encoding="utf-8") as handle:
            notices = handle.read()
    except OSError:
        notices = ""
        errors.append("THIRD_PARTY_NOTICES.md is missing")
    for comp in components:
        if comp.get("name") and comp["name"] not in notices:
            errors.append("%s is not named in THIRD_PARTY_NOTICES.md" % comp["name"])
    for item in informed_by:
        if item.get("name") and item["name"] not in notices:
            errors.append("informed-by project %s is not named in THIRD_PARTY_NOTICES.md" % item["name"])

    # Supply-chain pin: the wrapper jar is a binary executed on every build.
    for comp in components:
        expected = comp.get("sha256")
        for rel in comp.get("files", []):
            if expected and rel.endswith("gradle-wrapper.jar"):
                path = os.path.join(ROOT, rel)
                if not os.path.exists(path):
                    errors.append("%s: recorded file %s does not exist" % (comp["name"], rel))
                    continue
                with open(path, "rb") as handle:
                    actual = hashlib.sha256(handle.read()).hexdigest()
                if actual != expected:
                    errors.append("%s: %s sha256 %s != recorded %s" % (comp["name"], rel, actual, expected))
    props = os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.properties")
    try:
        with open(props, encoding="utf-8") as handle:
            if not re.search(r"^distributionSha256Sum=[0-9a-f]{64}$", handle.read(), re.M):
                errors.append("gradle-wrapper.properties has no distributionSha256Sum")
    except OSError:
        errors.append("gradle/wrapper/gradle-wrapper.properties is missing")

    if errors:
        print("third-party inventory: %d problem(s)" % len(errors))
        for err in errors:
            print("  - " + err)
        return 1
    extra = ", %d shipped runtime modules covered" % resolved_count if resolved_count is not None else ""
    print("third-party inventory ok: %d components, %d declared Gradle entries covered, %d informed-by%s"
          % (len(components), checked, len(informed_by), extra))
    return 0


if __name__ == "__main__":
    sys.exit(main())
