## Summary
- `SetToTagCommand` に pose 比較ログを追加し、`yagsl/fused/odom/selected/target` を同一周期で出力するようにした。
- `SetZeroCommand` に同形式の pose 比較ログを追加し、`yagsl/fused/odom/selected` と `deltaFO/useFused` を確認できるようにした。
- `SetZeroCommand` のログは `kStatusLogPeriodSec` 周期で出力し、既存制御ロジック（姿勢選択条件）は変更しない。

## Changed Files
- `src/main/java/frc/robot/commands/debug/odmetry/SetToTagCommand.java` - `[SetToTag][pose]` ログを追加。
- `src/main/java/frc/robot/commands/debug/odmetry/SetZeroCommand.java` - `[SetZero] periodic pose log` を追加。

## Review Status
- Reviewed by: pending
- Date: 2026-02-21
- Result: RECORDED
