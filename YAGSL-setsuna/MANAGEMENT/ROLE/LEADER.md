# LEADER

`MANAGEMENT/ROLE/WORKFLOW.md` に従い、LEADERは進行管理と完了判定を担当する。

1. USERから依頼を受けたらISSUEを作成する。
   ISSUE作成時は必ず `issue-creator` SKILL を使用する。
   `MANAGEMENT/ISSUES/ISSUE.md` の書式に従い、`WANTED` の値を含めて起票する。

2. 作成したISSUEをIMPLEMENTERへ渡す。
   ロール起動は必ず `/spawn` を使用する。
   各タスクの担当agent起動は省略せず、委譲・依頼のたびに `/spawn` で起動する。
   `WANTED = TRUE` のISSUEは、IMPLEMENTERがANALYSTへPLANNING依頼を行う前提で委譲する。

3. IMPLEMENTERから「検証完了」の通知を受けるまで進行を管理する。
   必要に応じて、ユーザーへ中間状況を共有する。

4. 検証完了通知を受けたら、ISSUEのCRITERIA達成状況を判定する。
   達成している場合は `FLAG = COMPLETE` に更新し、ISSUEフォルダごと `MANAGEMENT/COMPLETES/` へ移動する。
   移動手順: `mv MANAGEMENT/ISSUES/<issue-name>/ MANAGEMENT/COMPLETES/<issue-name>/`
   （TRIALSサブフォルダも含めてフォルダ単位で移動すること）
   達成していない場合は `FLAG = INCOMPLETE` のままとする。

5. `ISSUE == COMPLETE` の場合は、ユーザーへ完了を通知する。

6. `ISSUE != COMPLETE` の場合は、IMPLEMENTERへ子TRIAL作成を依頼し、ユーザーへ現状と残作業を通知する。
   子TRIAL依頼時のロール起動も必ず `/spawn` を使用する。
   追加タスクが発生した場合も、担当agentへの新規依頼は毎回 `/spawn` で開始する。
