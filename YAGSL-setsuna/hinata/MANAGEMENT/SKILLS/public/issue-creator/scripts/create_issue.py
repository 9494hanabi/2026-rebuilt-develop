#!/usr/bin/env python3
"""Create ISSUE markdown files for this repository's MANAGEMENT workflow."""

from __future__ import annotations

import argparse
from datetime import datetime
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create a MANAGEMENT issue file with the required template.",
    )
    parser.add_argument("--slug", required=True, help="Issue slug in hyphen-case.")
    parser.add_argument(
        "--where",
        action="append",
        required=True,
        help="Target file/directory path. Repeatable.",
    )
    parser.add_argument(
        "--assign",
        action="append",
        default=[],
        help="Assignment label. Repeatable.",
    )
    parser.add_argument("--summary-purpose", required=True, help="Purpose of request.")
    parser.add_argument("--summary-detail", required=True, help="Detail of request.")
    parser.add_argument(
        "--criteria",
        action="append",
        required=True,
        help="Measurable completion criterion. Repeatable.",
    )
    parser.add_argument(
        "--verification",
        required=True,
        help="How to verify criteria.",
    )
    parser.add_argument(
        "--wanted",
        default="FALSE",
        choices=["TRUE", "FALSE"],
        help="Wanted flag.",
    )
    parser.add_argument(
        "--flag",
        default="INCOMPLETE",
        choices=["COMPLETE", "INCOMPLETE"],
        help="Initial issue flag.",
    )
    parser.add_argument(
        "--timestamp",
        default=None,
        help="Timestamp in YYYY-MM-DD-HH-MM format. Default: current local time.",
    )
    parser.add_argument(
        "--base-dir",
        default="MANAGEMENT/ISSUES",
        help="Base directory where issue folder will be created.",
    )
    return parser.parse_args()


def validate_slug(slug: str) -> None:
    if not slug:
        raise ValueError("slug is required")
    allowed = set("abcdefghijklmnopqrstuvwxyz0123456789-")
    if any(ch not in allowed for ch in slug):
        raise ValueError("slug must be lowercase letters, digits, and hyphens only")


def resolve_timestamp(ts: str | None) -> str:
    if ts is None:
        return datetime.now().strftime("%Y-%m-%d-%H-%M")
    try:
        datetime.strptime(ts, "%Y-%m-%d-%H-%M")
    except ValueError as exc:
        raise ValueError("timestamp must match YYYY-MM-DD-HH-MM") from exc
    return ts


def bullet_lines(values: list[str], fallback: str | None = None) -> str:
    if values:
        return "\n".join(f"- {item}" for item in values)
    if fallback is not None:
        return f"- {fallback}"
    return "-"


def build_issue_content(args: argparse.Namespace) -> str:
    return "\n".join(
        [
            "## WHERE",
            bullet_lines(args.where),
            "",
            "## ASSIGN",
            bullet_lines(args.assign, fallback="lead"),
            "",
            "## SUMMARY",
            f"- {args.summary_purpose}",
            f"- {args.summary_detail}",
            "",
            "## CRITERIA",
            bullet_lines(args.criteria),
            "",
            "## VERIFICATION",
            f"- {args.verification}",
            "",
            "## RELATION",
            "- なし",
            "",
            "## WANTED",
            args.wanted,
            "",
            "## FLAG",
            f"FLAG = {args.flag}",
            "",
        ]
    )


def main() -> int:
    args = parse_args()
    validate_slug(args.slug)
    timestamp = resolve_timestamp(args.timestamp)

    issue_dir = Path(args.base_dir) / args.slug
    trials_dir = issue_dir / "TRIALS"
    issue_file = issue_dir / f"{args.slug}-issue-{timestamp}.md"

    issue_dir.mkdir(parents=True, exist_ok=True)
    trials_dir.mkdir(parents=True, exist_ok=True)

    if issue_file.exists():
        raise FileExistsError(f"Issue file already exists: {issue_file}")

    issue_file.write_text(build_issue_content(args), encoding="utf-8")

    print(f"Created issue file: {issue_file}")
    print(f"Created trials dir: {trials_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
