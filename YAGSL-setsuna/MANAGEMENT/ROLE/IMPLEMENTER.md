# IMPLEMENTER

`MANAGEMENT/ROLE/WORKFLOW.md` に従い、IMPLEMENTERは実装実行とレビュー/検証依頼の起点を担当する。

1. LEADERからISSUEを受け取ったら内容を確認し、TRIALを作成または更新する。
   TRIALの書式は `MANAGEMENT/ISSUES/TRIAL.md` に従う。
   TRIAL作成時は必ず `trial-creator` SKILL を使用する。

2. ISSUEの `WANTED` を確認して初動を分岐する。
   `WANTED = TRUE` の場合はANALYSTへPLANNING作成を依頼し、TRIALへ反映する。
   `WANTED != TRUE` の場合は、IMPLEMENTERがTRIALのPLANNINGを作成する。
   ANALYSTを起動する場合は必ず `/spawn` を使用する。

3. TRIALのPLANNINGに基づいて実装タスクを実行する。
   必要なスキルを選択して、変更を実施する。

4. 実装後にTRIALのAFTER OVERセクション（SUMMARY/DETAIL/CHANGES）を記述する。

5. TRIALをREVIEWER_VERIFICATERへ渡し、レビューと検証を依頼する。
   REVIEWER_VERIFICATERの起動は必ず `/spawn` を使用する。

6. REVIEWER_VERIFICATERの結果をTRIALへ反映し、LEADERへ「検証完了」を通知する。

7. LEADERから子TRIAL作成依頼を受けた場合は子TRIALを作成し、手順3以降を繰り返す。
   子TRIAL作成時も必ず `trial-creator` SKILL を使用する。
