#!/usr/bin/env python3
"""Create TRIAL markdown files for this repository's MANAGEMENT workflow."""

from __future__ import annotations

import argparse
from datetime import datetime
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create a MANAGEMENT trial file with the required template.",
    )
    parser.add_argument("--slug", required=True, help="Trial slug in hyphen-case.")
    parser.add_argument(
        "--parent",
        required=True,
        help="Path to parent ISSUE or TRIAL markdown file.",
    )
    parser.add_argument(
        "--timestamp",
        default=None,
        help="Timestamp in YYYY-MM-DD-HH-MM format. Default: current local time.",
    )
    parser.add_argument(
        "--status",
        default="PROGRESS",
        choices=["YET", "PROGRESS", "FINISH"],
        help="Initial TRIAL status.",
    )
    parser.add_argument(
        "--add-to-parent-relation",
        action="store_true",
        help="Append the generated trial path to parent\'s RELATION section.",
    )
    return parser.parse_args()


def validate_slug(slug: str) -> None:
    allowed = set("abcdefghijklmnopqrstuvwxyz0123456789-")
    if not slug or any(ch not in allowed for ch in slug):
        raise ValueError("slug must be lowercase letters, digits, and hyphens only")


def resolve_timestamp(ts: str | None) -> str:
    if ts is None:
        return datetime.now().strftime("%Y-%m-%d-%H-%M")
    try:
        datetime.strptime(ts, "%Y-%m-%d-%H-%M")
    except ValueError as exc:
        raise ValueError("timestamp must match YYYY-MM-DD-HH-MM") from exc
    return ts


def detect_trials_dir(parent_file: Path) -> Path:
    if parent_file.parent.name == "TRIALS":
        return parent_file.parent
    return parent_file.parent / "TRIALS"


def build_trial_content(parent_path: str, status: str, filename: str) -> str:
    lines = [
        f"# {filename}",
        "",
        "## 0.PARENT ISSUE/TRIAL",
        f"- {parent_path}",
        "",
        "## ENVIRONMENT",
        "- TODO",
        "",
        "## HYPOTHESIS",
        "- TODO",
        "",
        "## SOLUTION",
        "- TODO",
        "",
        "## PLAN",
        "- TODO",
        "",
        "## CRITERIA",
        "- TODO",
        "",
        "## VERIFICATION",
        "- TODO",
        "",
        "## SUMMARY",
        "- TODO",
        "",
        "## DETAIL",
        "- TODO",
        "",
        "## CHANGES",
        "- TODO",
        "",
        "## REVIEW SUMMARY",
        "- TODO",
        "",
        "## VERIFICATION SUMMARY",
        "- TODO",
        "",
        "### DISCOVERY",
        "- TODO",
        "",
        "### REFUTED HYPOTHESES",
        "- TODO",
        "",
        "### SUPPORTED HYPOTHESES",
        "- TODO",
        "",
        "## RELATION",
        "- なし",
        "",
        "## STATUS",
        status,
        "",
    ]
    return "\n".join(lines)


def append_relation(parent_file: Path, trial_rel_path: str) -> None:
    text = parent_file.read_text(encoding="utf-8")
    marker = "## RELATION"
    if marker not in text:
        return

    lines = text.splitlines()
    for i, line in enumerate(lines):
        if line.strip() == marker:
            insert_at = i + 1
            while insert_at < len(lines) and lines[insert_at].strip().startswith("-"):
                insert_at += 1
            lines.insert(insert_at, f"- {trial_rel_path}")
            parent_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
            return


def main() -> int:
    args = parse_args()
    validate_slug(args.slug)
    timestamp = resolve_timestamp(args.timestamp)

    parent_file = Path(args.parent)
    if not parent_file.exists():
        raise FileNotFoundError(f"Parent file not found: {parent_file}")

    trials_dir = detect_trials_dir(parent_file)
    trials_dir.mkdir(parents=True, exist_ok=True)

    filename = f"{args.slug}-trial-{timestamp}.md"
    trial_file = trials_dir / filename
    if trial_file.exists():
        raise FileExistsError(f"Trial file already exists: {trial_file}")

    parent_ref = parent_file.as_posix()
    content = build_trial_content(parent_ref, args.status, filename)
    trial_file.write_text(content, encoding="utf-8")

    trial_rel_path = trial_file.as_posix()
    if args.add_to_parent_relation:
        append_relation(parent_file, trial_rel_path)

    print(f"Created trial file: {trial_file}")
    if args.add_to_parent_relation:
        print(f"Updated parent RELATION: {parent_file}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
