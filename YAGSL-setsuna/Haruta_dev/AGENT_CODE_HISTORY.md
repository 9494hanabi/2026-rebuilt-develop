# AGENT_CODE_HISTORY

AGENT がコード提案を出したときだけ履歴を残すファイル。

## Rule
- コードを生成していない chat では記録しない
- 説明だけ、調査だけ、相談だけの chat では記録しない
- コードブロックや具体的な変更差分を提案したときだけ記録する
- 1 回の提案ごとに 1 エントリを追加する

## Entry Template
## YYYY-MM-DD HH:MM
- 目的:
- 対象ファイル:
- 提案したコードの概要:
- 変更理由:
- 確認方法:
- 未解決点:

## 2026-03-01 18:12
- 目的: Fuel の単体追尾を塊追尾に変更し、既存4ファイルだけで実験できる案を出す
- 対象ファイル: AutoVisionConstants.java / AutoIntakeAssistCommand.java / AutoVisionCommand.java / AutoBindings.java
- 提案したコードの概要: rawdetections ベースの cluster 選択を AutoIntakeAssistCommand 内に実装し、timeout を保険として維持する最小構成
- 変更理由: 1個追尾では Fuel が多い方向へ入れず、新規ファイルを増やしすぎると混乱しやすいため
- 確認方法: test.auto と SmartDashboard で cluster area / tx / accepted count / last seen hold を確認する
- 未解決点: detector classId と area 閾値は実機ログで要調整

## 2026-03-01 19:05
- 目的: Hailo-8 前提の PieceVisionSubsystem を追加し、Fuel 検出の責務を subsystem と command に分離する案を出す
- 対象ファイル: PieceVisionConstants.java / PieceVisionSubsystem.java / AutoVisionConstants.java / RobotContainer.java / AutoBindings.java / AutoVisionCommand.java / AutoIntakeAssistCommand.java
- 提案したコードの概要: piece 検出を `subsystems/vision/PieceVisionSubsystem` に移し、Auto 側は cluster へ向かう行動と fallback path 制御に集中する構成
- 変更理由: Command-Based の責務分離に沿って、検出機能と Auto 行動を分けた方が保守性と安全性が高いため
- 確認方法: `limelight-intake` の dashboard 値、`visionAcquirePiece` の cluster 追従、target 未検出時の `back` path 遷移を確認する
- 未解決点: Hailo detector の classId、面積閾値、cluster 重みは実機ログで要調整

## 2026-03-01 19:32
- 目的: PieceVision 定数を AutoVisionConstants に統合し、AutoBindings の旧 piece 登録と定数名ズレを解消する案を出す
- 対象ファイル: AutoVisionConstants.java / PieceVisionSubsystem.java / AutoVisionCommand.java / AutoIntakeAssistCommand.java / AutoBindings.java / PieceVisionConstants.java
- 提案したコードの概要: `PieceVisionConstants.java` を廃止し、piece camera / detector filter / cluster score の定数を `AutoVisionConstants.java` に集約したうえで、piece subsystem と auto command の参照先を一本化する構成
- 変更理由: constants だけ別ファイルに分けるより、今の規模では `AutoVisionConstants.java` にまとまっていた方が Haruta が追いやすく、`AutoBindings.java` の旧 registration も同時に整理できるため
- 確認方法: `./gradlew compileJava` が通ること、`visionPieceModeOn` と `visionAcquirePiece` が重複登録されていないこと、`Vision/Piece/*` の dashboard 値が出ること
- 未解決点: Hailo detector の class id、detector pipeline 番号、Fuel 面積閾値は実機ログで要調整

## 2026-03-01 19:45
- 目的: AutoBindings に残っている重複した piece NamedCommands 登録を整理する案を出す
- 対象ファイル: AutoBindings.java
- 提案したコードの概要: `visionPieceModeOn` と `visionAcquirePiece` の二重登録のうち、後ろ側の重複ブロックを削除して 1 セットだけ残す
- 変更理由: compile は通っていても NamedCommands の登録が二重だと、将来の挙動確認とデバッグがやりにくくなるため
- 確認方法: `visionPieceModeOn` と `visionAcquirePiece` が各1回だけ現れること、`./gradlew compileJava` が通ること
- 未解決点: その後に `RobotContainer` の `piecevision` 命名整理と `AutoVisionCommand` の未使用 import 整理を行うと読みやすくなる

## 2026-03-01 19:54
- 目的: RobotContainer で piece 用 subsystem 変数名を分かりやすくする案を出す
- 対象ファイル: RobotContainer.java
- 提案したコードの概要: `piecevision` を `pieceVisionSubsystem` に変更し、field / 生成 / AutoBindings への受け渡しを統一する
- 変更理由: piece 用 subsystem の役割が名前だけで分かるようにし、Haruta が追いやすくするため
- 確認方法: `RobotContainer.java` 内の `piecevision` が消えること、`./gradlew compileJava` が通ること
- 未解決点: その後に `AutoBindings` のコンストラクタ引数名や `AutoVisionCommand` の未使用 import を整理するとさらに読みやすくなる

## 2026-03-01 20:08
- 目的: ShooterSubsystem の理解を進めるため、入力・処理・出力が追いやすいコメント追加案を出す
- 対象ファイル: ShooterSubsystem.java
- 提案したコードの概要: 定数、状態、constructor、setTargetRps、Ready 判定、periodic の各塊に理解用コメントを追加する
- 変更理由: Haruta が subsystem の責務とデータの流れを 1 ファイル内で追えるようにするため
- 確認方法: ShooterSubsystem を上から読んだ時に「設定」「目標設定」「Ready 判定」「ダッシュボード出力」の流れが分かること
- 未解決点: コメント追加後に AutoCommand / AutoBindings / DebugBindings とのつながりも順番に整理すると理解が深まる
