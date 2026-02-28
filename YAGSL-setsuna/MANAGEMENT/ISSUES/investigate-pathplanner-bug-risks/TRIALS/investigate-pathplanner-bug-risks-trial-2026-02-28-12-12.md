# investigate-pathplanner-bug-risks-trial-2026-02-28-12-12.md

## 0.PARENT ISSUE/TRIAL
- /Users/suzukiakiramuki/Hanabi/2026-develop/2026-rebuilt-develop/YAGSL-setsuna/MANAGEMENT/ISSUES/investigate-pathplanner-bug-risks/investigate-pathplanner-bug-risks-issue-2026-02-28-12-12.md

## ENVIRONMENT
- ローカルリポジトリ上で静的調査を実施
- `./gradlew compileJava` を実行し、ビルド整合性を確認
- このセッションには別エージェントへ直接メッセージを送る手段がないため、ANALYST / REVIEWER_VERIFICATER 依頼は保留

## HYPOTHESIS
- Auto chooser の設定名と実在する `.auto` 名の不一致で、意図した Auto が既定選択されていない可能性がある
- SmartDashboard 上の chooser が二重定義され、ユーザーの選択が実行系に反映されていない可能性がある
- `src/main/java/frc/robot/lib/pathplanner/` 配下は調査対象だが、実行系に含まれていない可能性がある

## SOLUTION
- PathPlanner 関連コードと deploy 設定を読み、Auto 選択・Path 読み込み・イベント登録の整合性を確認する
- PathPlannerLib のソース実装も参照し、chooser のデフォルト挙動と `PathPlannerAuto` の例外条件を確認する
- 結果を TRIAL に整理し、追加検証が必要な点を明記する

## PLAN
- `trial-creator` で作成済みの TRIAL を初期化する
- WANTED=TRUE のため本来は ANALYST に PLANNING を依頼すべきだが、このセッションからは別エージェントへ送信できないため、代替として IMPLEMENTER が暫定 PLANNING を作成する
- `AutoCommand.java`、`RobotContainer.java`、`SwerveSubsystem.java`、`AutoBindings.java`、`deploy/pathplanner` を確認する
- `build.gradle` と PathPlannerLib ソースを確認し、`lib/pathplanner` 配下が実行時に影響するかを切り分ける
- `./gradlew compileJava` でビルドを確認し、調査結果を AFTER OVER に記録する

## CRITERIA
- PathPlanner 関連の主要実装と設定について、潜在的なバグ要因の有無を結論付きで整理できている
- バグ要因候補ごとに根拠ファイルと該当箇所を示せている
- 必要に応じて追加検証手順を示せている

## VERIFICATION
- 該当ソースと設定ファイルを読み、結論がコード上の根拠と一致していること
- `./gradlew compileJava` が成功し、少なくとも現行コードがビルド不能ではないこと
- 追加の実機/Sim 検証が必要な項目を区別していること

## SUMMARY
- PathPlanner 周りを調査した結果、Auto chooser 周辺に実運用上の不具合要因が 2 件あり、`lib/pathplanner` 配下そのものは現行ビルド対象外で直接原因ではなかった

## DETAIL
- 主要な不具合要因:
- 1) `RobotContainer` は PathPlanner の `SendableChooser<Command>` を `"Auto setting"` へ公開しているが、直後に `Robot` が同じキーへ別の `SendableChooser<String>` を公開して上書きしている。結果としてダッシュボードから触れる chooser と、`autonomousInit()` で実際に使う chooser が一致していない
- 2) `RobotContainer` の `buildAutoChooser("New Auto")` は既定 Auto 名に `"New Auto"` を指定しているが、リポジトリ上に存在する Auto は `New New Auto.auto`。PathPlannerLib の `AutoBuilder.buildAutoChooser(String)` は存在しない既定名を渡すと `None` を既定にするため、意図した Auto が既定選択されない
- 補足:
- `AutoCommand.pathPlannerAuto()` は `drivebase.getAutonomousCommand(autoName)` をそのまま呼ぶため、`AutoBuilder` 未設定時には `PathPlannerAuto` 生成で例外化し得る。ただし現時点でこのヘルパーの外部使用箇所は見当たらないため、現状は潜在リスクに留まる
- `src/main/java/frc/robot/lib/pathplanner/` 配下は `build.gradle` で production compile から除外されているため、ここにある実験コードは現行ランタイムの直接原因ではない
- `deploy/pathplanner` 配下の `start.path` / `middle.path` で使われている `markStart` / `shooterZoneSpin` は `AutoBindings` 側の NamedCommands 登録と整合していた

## CHANGES
- `MANAGEMENT/ISSUES/investigate-pathplanner-bug-risks/TRIALS/investigate-pathplanner-bug-risks-trial-2026-02-28-12-12.md`
- 調査計画、静的解析結果、検証結果、未実施のチーム連携状況を記録

## REVIEW SUMMARY
- REVIEWER_VERIFICATER 依頼は未実施
- 理由: このセッションから別エージェント ID 宛に直接メッセージを送る手段が提供されていないため

## VERIFICATION SUMMARY
- IMPLEMENTER による暫定検証を実施
- `./gradlew compileJava` : 成功
- 静的確認により、`deploy/pathplanner` の主要ファイルは存在し、イベント名の不整合は見つからなかった

### DISCOVERY
- ANALYST 依頼未実施のため未記入（セッション制約）

### REFUTED HYPOTHESES
- ANALYST 依頼未実施のため未記入（セッション制約）

### SUPPORTED HYPOTHESES
- ANALYST 依頼未実施のため未記入（セッション制約）

## RELATION
- なし

## STATUS
PROGRESS
