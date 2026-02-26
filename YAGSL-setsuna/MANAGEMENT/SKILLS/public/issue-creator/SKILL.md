---
name: issue-creator
description: Create MANAGEMENT issue folders and issue markdown files that follow MANAGEMENT/ISSUES/ISSUE.md rules. Use when a new user request arrives and an ISSUE must be filed with required sections, filename format, and default flags.
---

# Issue Creator

Create a new ISSUE under `MANAGEMENT/ISSUES/<issue-name>/` using the project's required format.

## Run This Skill

1. Collect required inputs.
- `slug`: short hyphen-case identifier (example: `add-login-page`)
- `where`: one or more target file paths
- `summary_purpose`: purpose of the request
- `summary_detail`: request details
- `criteria`: measurable completion criteria (1+)
- `verification`: how criteria will be judged

2. Run the generator script.
```bash
python3 scripts/create_issue.py \
  --slug "<slug>" \
  --where "<file-or-dir>" \
  --summary-purpose "<purpose>" \
  --summary-detail "<detail>" \
  --criteria "<criterion-1>" \
  --verification "<how-to-verify>"
```

3. If assignment or wanted state is known, set options.
```bash
python3 scripts/create_issue.py \
  --slug "<slug>" \
  --where "<file-or-dir>" \
  --summary-purpose "<purpose>" \
  --summary-detail "<detail>" \
  --criteria "<criterion-1>" \
  --verification "<how-to-verify>" \
  --assign lead \
  --assign impl-core \
  --wanted FALSE
```

4. Verify output path and contents.
- Confirm file path: `MANAGEMENT/ISSUES/<slug>/<slug>-issue-YYYY-MM-DD-HH-MM.md`
- Confirm sections: `WHERE/ASSIGN/SUMMARY/CRITERIA/VERIFICATION/RELATION/WANTED/FLAG`
- Confirm defaults: `RELATION` contains `なし`, `FLAG = INCOMPLETE`

## Notes

- Keep `criteria` measurable.
- Use `--timestamp` only when deterministic reproduction is required.
- The script creates the parent directory and `TRIALS/` directory automatically.
