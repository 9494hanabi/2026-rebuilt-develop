# TRIALについて

file name format : summary-trial-2026-01-01-23-59.md
summary部分はtrialの内容を完結に示せ。

TRIAL作成時は必ず `trial-creator` SKILL を使用すること。

```markdown
# TRIALのファイル名

## 0.PARENT ISSUE/TRIAL
- 親となるISSUEまたはTRIALの名前。

<!-- 実行前に記述する。-->
<!-- PLANNING SECTION-->
## ENVIRONMENT
- 実装上の問題を解決する場合は問題が発生した時の環境を記述する。TODOの場合はスキップ。

## HYPOTHESIS
- 実装上の問題を解決する場合は問題の原因を推定して記述する。TODOの場合はスキップ。

## SOLUTION
- 実装上の問題を解決する方法を記述する。TODOの場合はスキップ。

## PLAN
- 実行に際する計画を立てる。
- SOLUTIONを実行する手順、使用するツールなどを記述する。

## CRITERIA
- 達成基準を記述する
- 達成基準は必ず測定可能なものでなければならない

## VERIFICATION
- CRITERIAの判定基準

<!-- TRIALを実行後に記述する -->
<!-- AFTER OVER SECTION-->
## SUMMARY
- 変更の要約/目的

## DETAIL
- 実装の詳細を記述する。

## CHANGES
- 変更箇所のfile path一覧
- ...

- 簡単な変更の場合
- filename.ex
2 1行ずつ
3 変更を示す


- 大規模な変更の場合
filename.ex
1
2 コードブロックで
3 変更を示す
4


<!-- REVIEWER_VERIFICATERが記述する。 -->
## REVIEW SUMMARY

- レビューの結果をまとめる。

## VERIFICATION SUMMARY

- VERIFICATIONの実行結果をまとめる。

<!-- ANALYSTが記述する。 -->
### DISCOVERY
- 検証によって発見された事実

### REFUTED HYPOTHESES
- 検証の結果、誤りだと判明した仮説・仮定（事前に想定していたが実際には成り立たなかったもの）

### SUPPORTED HYPOTHESES
- 検証の結果、正しいと裏付けられた仮説・仮定（事前の想定が実際に確認されたもの）

<!-- 実行後新たな問題/タスクが発生した場合はTRIALを作成し、ここにRELATIONとして記述する。 -->
## RELATION
- 子TRIAL

## STATUS
YET/PROGRESS/FINISH

```

TRIALを書いたら、親ISSUEのRELATIONに作成したTRIALを追加しなさい。
