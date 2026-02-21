## Summary
- `SetZeroCommand` のドライブ制御を `DriveControllCommand` に統合し、制御ロジックの重複を削減した。
- `DriveControllCommand` に速度上限（並進/回頭）を外部から渡せるコンストラクタを追加し、コマンドごとの速度設定を可能にした。
- `OdomTranslationController` の X/Y 計算不整合を修正し、Y軸PID・最小有効速度補正・`ControlResult` の targetY 反映を正した。

## Changed Files
- `src/main/java/frc/robot/commands/debug/odmetry/SetZeroCommand.java` - `DriveControllCommand` 継承へ変更し、SetZero用の現在姿勢選択（fused優先/headingはodom）を `Supplier<Pose2d>` で注入。
- `src/main/java/frc/robot/commands/debug/odmetry/DriveControllCommand.java` - 速度上限を受け取るオーバーロードコンストラクタ追加、初期化時の `translationControl.reset()` 追加。
- `src/main/java/frc/robot/lib/util/OdomTranslationController.java` - Translation計算の軸取り違え修正、しきい値補正の適用先修正、`ControlResult` への targetY 設定修正。

## Review Status
- Reviewed by: reviewer
- Date: 2026-02-21
- Result: APPROVED (Note: `SetToTagCommandRefactored.java` に未解決の既存コンパイルエラーは別件として残存)
