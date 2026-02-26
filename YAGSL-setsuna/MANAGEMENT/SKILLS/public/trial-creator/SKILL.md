---
name: trial-creator
description: Create MANAGEMENT trial markdown files that follow MANAGEMENT/ISSUES/TRIAL.md rules. Use when IMPLEMENTER creates or updates a TRIAL for a parent ISSUE/TRIAL, including child TRIAL creation loops.
---

# Trial Creator

Create a new TRIAL file under the correct `TRIALS/` directory with the required sections.

## Run This Skill

1. Collect required inputs.
- `slug`: short hyphen-case identifier (example: `fix-render-timeout`)
- `parent`: parent ISSUE or TRIAL markdown path

2. Run the generator script.
```bash
python3 scripts/create_trial.py \
  --slug "<slug>" \
  --parent "<path-to-parent-issue-or-trial.md>"
```

3. Optionally control timestamp or parent RELATION update.
```bash
python3 scripts/create_trial.py \
  --slug "<slug>" \
  --parent "<path-to-parent-issue-or-trial.md>" \
  --timestamp "YYYY-MM-DD-HH-MM" \
  --add-to-parent-relation
```

4. Verify output.
- Confirm file path: `<issue-dir>/TRIALS/<slug>-trial-YYYY-MM-DD-HH-MM.md`
- Confirm parent path is written under `## 0.PARENT ISSUE/TRIAL`
- Confirm default status is `PROGRESS`

## Notes

- Use measurable criteria and verification statements after generation.
- Use `--add-to-parent-relation` to append the new TRIAL path to the parent file's `## RELATION` section.
- Script creates the target `TRIALS/` directory automatically when needed.
