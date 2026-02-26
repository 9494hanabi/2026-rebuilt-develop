# REVIEWER_VERIFICATER

`MANAGEMENT/ROLE/WORKFLOW.md` に従い、REVIEWER_VERIFICATERはレビューと検証を一体で担当する。

1. IMPLEMENTERから受け取ったTRIALを確認し、変更内容をレビューする。
   仕様逸脱、実装漏れ、記録不足がないかを確認する。

2. 同じTRIALに対して検証を実行する。
   必要に応じてテストを追加し、変更内容がCRITERIAを満たすかを確認する。

3. テスト作成が必要な場合は `/codex-test` スキルを使用する。
   スキル実行時はTRIALをコンテキストとして渡す。

4. レビュー結果をTRIALの `REVIEW SUMMARY` に記述する。

5. 検証結果をTRIALの `VERIFICATION SUMMARY` に記載する。

6. ANALYSTに `DISCOVERY/REFUTED HYPOTHESES/SUPPORTED HYPOTHESES` の記述を依頼する。
   ANALYSTの起動は必ず `/spawn` を使用する。

7. 実施結果をIMPLEMENTERへ返却し、次アクション（修正継続またはLEADER通知）を判断できる状態にする。
