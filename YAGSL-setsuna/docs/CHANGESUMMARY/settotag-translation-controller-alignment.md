## Summary
- `SetToTagCommand` の並進制御を `OdomTranslationController` 経由に統一し、`DriveControllCommand` 系と同じ符号キャリブレーション経路を使うようにした。
- `SetToTagCommand` の停止系 early return で `translationControl.reset()` を追加し、制御器状態のリセットを一貫化した。
- `./gradlew compileJava -q` でコンパイル通過を確認した。

## Changed Files
- `src/main/java/frc/robot/commands/debug/odmetry/SetToTagCommand.java` - 並進出力計算を直接PID計算から `OdomTranslationController` に置換し、`atSetpoint` 判定も `ControlResult` ベースへ変更。

## Review Status
- Reviewed by: pending
- Date: 2026-02-21
- Result: RECORDED (Note: 実機で座標反転が継続しているため、根本原因は未解決)
