# Limelight dual ON と ID飛び対策 調査メモ

作成日: 2026-02-08  
前提: deploy条件は daisha 側に合わせて運用されているものとして分析

## 1. 問い
「Limelight の dual（マルチタグ）を ON にしていれば、`tid` の飛びや `targetpose_robotspace` の飛びは解消されるか？」

## 2. 結論
- 結論は **No（十分ではない）**。
- dual ON は主に `botpose` 系（MegaTag/MegaTag2）の安定化に効く。
- 一方で `tid` と `targetpose_robotspace` は「その時点の primary in-view tag」に依存するため、2タグ視野で primary が切り替わると値が飛ぶ。

## 3. 公式ドキュメント根拠

1. `tid` と `targetpose_robotspace` は primary tag 基準
- Limelight NetworkTables API は `tid` を「primary in-view AprilTag ID」と定義。
- `targetpose_robotspace` / `targetpose_cameraspace` / `botpose_targetspace` も primary in-view AprilTag 基準。
- 参照: https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api

2. `priorityid` は localization を固定しない
- `priorityid` は tx/ty ターゲティング用で、localization には影響しないと明記。
- 参照: https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api
- 参照: https://www.chiefdelphi.com/t/limelight-2024-1/456017

3. 複数タグの高度利用は JSON/Fiducial を使う
- Limelight docs は複数タグの高度用途で JSON dump の利用を案内。
- JSON仕様では `Fiducial` 配列にタグごとの `fID`, `t6t_rs`, `t6r_fs(_orb)` が出る。
- 参照: https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags
- 参照: https://docs.limelightvision.io/docs/docs-limelight/apis/json-results-specification

4. FRC向けに IDフィルタ活用が推奨される
- Limelight docs の AprilTag Tips では、多くのFRCチーム向けに「各pipelineは1つのtag ID追跡」が推奨されている。
- 参照: https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags

5. MegaTag2 は「有効IDだけ使う」運用を想定
- MT2ページで `SetFiducialIDFiltersOverride` による動的フィルタ例が示されている。
- 参照: https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltag-robot-localization-megatag2

## 4. FRCコミュニティ（ChiefDelphi）での実務傾向

1. 2タグ見えて誤ったタグを掴む場合
- 対策として「タグIDごとのpipeline切替」または「可視fiducial配列からID選択」が共有されている。
- 参照: https://www.chiefdelphi.com/t/when-multiple-april-tags-are-in-sight-through-the-limelight-how-can-i-code-it-so-that-it-can-only-focus-on-one-on-the-april-tags/453245

2. `priorityid` の位置づけ
- 2024.1 の告知でも `priorityid` は tx/ty用で localization 非対象という説明。
- 参照: https://www.chiefdelphi.com/t/limelight-2024-1/456017

3. 「飛び」に見える別要因も多い
- タグ配置/field map不一致が multi-tag不整合の原因になった事例あり。
- 参照: https://www.chiefdelphi.com/t/limelight-apriltag-detection-inaccurate-with-multiple-tags/452019
- MT2利用時の `SetRobotOrientation` yaw ずれ（180度ずれ）で姿勢が逆側に出る事例あり。
- 参照: https://www.chiefdelphi.com/t/limelight-puts-field-pose-backwards-ll3g/468570

## 5. あなたの `B/X` への示唆（現実的な順序）

1. 最優先: コマンド中の tag ID ロック
- 開始時に採用したIDを保持し、終了まで切り替えない。
- 一定時間見失ったら停止または再取得。

2. `tid` 直参照を減らし、Fiducial配列から選ぶ
- `getLatestResults(...).targetingResults.targets_Fiducials` や JSON `Fiducial[]` から、ID・面積・距離・履歴で選択。

3. ヒステリシスを入れる
- 「新IDへ切替する条件」を厳しくする（面積差、連続Nフレーム、時間しきい値）。

4. pipeline/IDフィルタ戦略
- 目的地が明確な局面では IDフィルタ or pipeline切替で候補ID集合を絞る。
- MT2で動的フィルタも可。

5. `priorityid` の使い分け
- 2D照準（tx/ty）用途には有効。
- localization/絶対位置決めの安定化目的には単独では不十分。

## 6. 実装時の注意
- `dual ON` は維持してよいが、「primary依存値（`tid`, `targetpose_robotspace`）をそのまま制御入力にする」設計は残る。
- フィールドマップ、タグ実配置、`SetRobotOrientation` の符号/基準を再確認しないと、ID飛び対策後も異常挙動は残る。

