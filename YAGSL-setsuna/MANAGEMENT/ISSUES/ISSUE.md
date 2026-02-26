# ISSUESについて

ユーザーからの依頼について必ずISSUEを立てること。

file name format : summary-issue-2026-01-01-23-59.md
summary部分はissueの内容を完結に示せ。

```markdown
## WHERE
- file path

## ASSIGN
- ex)
- lead
- impl-frontend

## SUMMARY
- 依頼の目的
- 依頼の内容

## CRITERIA
- 達成基準を記述する
- 達成基準は必ず測定可能なものでなければならない

## VERIFICATION
- CRITERIAの判定方法を記述する。

## RELATION
- 関連するTRIAL一覧

## WANTED
TRUE

## FLAG
FLAG =  COMPLETE/INCOMPLETE

```

タスク完了した場合は

```markdown
FLAG = COMPLETE
```

に変更しMANAGEMENT/COMPLETES/にissueを移動する。

RELATIONが5つを超えた場合はWANTEDにissueを追加する。

作成したISSUEは
ISSUES/issue-name/
を作成し、そこに配置しなさい。
