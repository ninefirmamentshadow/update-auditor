#!/usr/bin/env python3
"""Branch handoff helper (stdlib only).

One handoff file per active branch at .agent/handoffs/<branch-with-slashes-as-dashes>.json,
shaped by .agent/handoff.schema.json. Agents write `claimed`, `pending` and
`next_action` by hand. Only scripts/verify.sh calls `record`, which is the sole
writer of `verified` and `head_commit`, so claimed work stays separate from
verified work.

Usage:
  handoff.py path [--branch B]              print the handoff path for a branch
  handoff.py validate [FILE ...]            validate handoff files against the schema
  handoff.py record --tsv FILE --commit SHA [--branch B]
                                            replace `verified` from verify.sh results
                                            (tab-separated: check, command, result, detail)
"""
import argparse
import datetime
import json
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SCHEMA = os.path.join(ROOT, ".agent", "handoff.schema.json")
HANDOFF_DIR = os.path.join(ROOT, ".agent", "handoffs")


def git(*args, check=True):
    out = subprocess.run(["git", "-C", ROOT, *args], capture_output=True, text=True)
    if check and out.returncode != 0:
        raise SystemExit("git %s failed: %s" % (" ".join(args), out.stderr.strip()))
    return out.stdout.strip()


def current_branch():
    return git("rev-parse", "--abbrev-ref", "HEAD")


def handoff_path(branch):
    return os.path.join(HANDOFF_DIR, branch.replace("/", "-") + ".json")


def now_iso():
    return datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


# --- minimal JSON Schema subset validator --------------------------------------
# Supports exactly what handoff.schema.json uses: type (incl. type lists), required,
# additionalProperties:false, properties, items, enum, pattern, minimum, and a
# date-time sanity check. Anything else in the schema is ignored, not trusted.

def _type_ok(value, expected):
    names = expected if isinstance(expected, list) else [expected]
    for name in names:
        if name == "string" and isinstance(value, str):
            return True
        if name == "integer" and isinstance(value, int) and not isinstance(value, bool):
            return True
        if name == "array" and isinstance(value, list):
            return True
        if name == "object" and isinstance(value, dict):
            return True
        if name == "null" and value is None:
            return True
        if name == "boolean" and isinstance(value, bool):
            return True
    return False


def validate(value, schema, path="$"):
    errors = []
    if "type" in schema and not _type_ok(value, schema["type"]):
        return ["%s: expected %s" % (path, schema["type"])]
    if "enum" in schema and value not in schema["enum"]:
        errors.append("%s: %r not in %s" % (path, value, schema["enum"]))
    if isinstance(value, str):
        if "pattern" in schema and not re.search(schema["pattern"], value):
            errors.append("%s: %r does not match %s" % (path, value, schema["pattern"]))
        if schema.get("format") == "date-time":
            try:
                datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
            except ValueError:
                errors.append("%s: %r is not an ISO date-time" % (path, value))
    if isinstance(value, int) and not isinstance(value, bool) and "minimum" in schema:
        if value < schema["minimum"]:
            errors.append("%s: %d < minimum %d" % (path, value, schema["minimum"]))
    if isinstance(value, dict):
        props = schema.get("properties", {})
        for key in schema.get("required", []):
            if key not in value:
                errors.append("%s: missing required %r" % (path, key))
        if schema.get("additionalProperties") is False:
            for key in value:
                if key not in props:
                    errors.append("%s: unexpected property %r" % (path, key))
        for key, sub in props.items():
            if key in value:
                errors.extend(validate(value[key], sub, "%s.%s" % (path, key)))
    if isinstance(value, list) and "items" in schema:
        for index, item in enumerate(value):
            errors.extend(validate(item, schema["items"], "%s[%d]" % (path, index)))
    return errors


def load_schema():
    with open(SCHEMA, encoding="utf-8") as handle:
        return json.load(handle)


def cmd_validate(files):
    if not files:
        files = sorted(
            os.path.join(HANDOFF_DIR, name)
            for name in os.listdir(HANDOFF_DIR)
            if name.endswith(".json")
        ) if os.path.isdir(HANDOFF_DIR) else []
    if not files:
        print("no handoff files to validate")
        return 0
    schema = load_schema()
    failed = False
    for path in files:
        try:
            with open(path, encoding="utf-8") as handle:
                data = json.load(handle)
        except (OSError, ValueError) as exc:
            print("FAIL %s: %s" % (path, exc))
            failed = True
            continue
        errors = validate(data, schema)
        if errors:
            failed = True
            print("FAIL %s" % path)
            for err in errors:
                print("  " + err)
        else:
            print("ok   %s" % os.path.relpath(path, ROOT))
    return 1 if failed else 0


def skeleton(branch, commit):
    remote = git("config", "--get", "remote.origin.url", check=False)
    repo = os.path.basename(remote.rstrip("/")).removesuffix(".git") if remote else os.path.basename(ROOT)
    base = git("merge-base", "origin/main", "HEAD", check=False) or commit
    return {
        "repo": repo,
        "objective": "TODO: state this branch's objective",
        "branch": branch,
        "base_commit": base,
        "head_commit": commit,
        "owner_agent": "unassigned",
        "claimed": [],
        "verified": [],
        "not_run": [],
        "pending": [],
        "artifacts": [],
        "next_action": "TODO: state the next action",
        "next_executor_hint": "coding-model",
        "opened_at": now_iso(),
        "closed_at": None,
        "owner_interventions": 0,
    }


def cmd_record(tsv, commit, branch):
    branch = branch or current_branch()
    path = handoff_path(branch)
    os.makedirs(HANDOFF_DIR, exist_ok=True)
    if os.path.exists(path):
        with open(path, encoding="utf-8") as handle:
            data = json.load(handle)
    else:
        data = skeleton(branch, commit)
    verified = []
    with open(tsv, encoding="utf-8") as handle:
        for line in handle:
            line = line.rstrip("\n")
            if not line:
                continue
            check, command, result, detail = (line.split("\t") + ["", "", "", ""])[:4]
            entry = {"check": check, "command": command, "result": result, "commit": commit}
            detail = detail.strip()
            if detail:
                entry["detail"] = detail
            verified.append(entry)
    data["verified"] = verified
    data["head_commit"] = commit
    errors = validate(data, load_schema())
    if errors:
        print("handoff NOT written; it would not match the schema:", file=sys.stderr)
        for err in errors:
            print("  " + err, file=sys.stderr)
        return 1
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")
    os.replace(tmp, path)
    print("handoff written: %s" % os.path.relpath(path, ROOT))
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)
    p_path = sub.add_parser("path")
    p_path.add_argument("--branch")
    p_val = sub.add_parser("validate")
    p_val.add_argument("files", nargs="*")
    p_rec = sub.add_parser("record")
    p_rec.add_argument("--tsv", required=True)
    p_rec.add_argument("--commit", required=True)
    p_rec.add_argument("--branch")
    args = parser.parse_args()
    if args.cmd == "path":
        print(handoff_path(args.branch or current_branch()))
        return 0
    if args.cmd == "validate":
        return cmd_validate(args.files)
    return cmd_record(args.tsv, args.commit, args.branch)


if __name__ == "__main__":
    sys.exit(main())
