## WHERE
- MANAGEMENT/SKILLS/public/
- ~/.agent/skills/

## ASSIGN
- lead

## SUMMARY
- MANAGEMENTスキルをClaude Code / Codex-CLIに登録する
- MANAGEMENT/SKILLS/public/ 配下の issue-creator, trial-creator, spawn-team を ~/.agent/skills/ にコピーし、エージェントがスキルとして発見・使用できるようにする

## CRITERIA
- ~/.agent/skills/issue-creator/SKILL.md が存在する
- ~/.agent/skills/trial-creator/SKILL.md が存在する
- ~/.agent/skills/spawn-team/SKILL.md が存在する
- 各スキルのscripts/ディレクトリとagents/ディレクトリが正しくコピーされている

## VERIFICATION
- ls -la ~/.agent/skills/issue-creator/ ~/.agent/skills/trial-creator/ ~/.agent/skills/spawn-team/ で存在を確認する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = COMPLETE
