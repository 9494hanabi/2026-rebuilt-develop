## Summary
- SetToTagCommandをRefactored版のロジックに統一し、SetToTagCommandRefactored.javaを削除
- resolveTargetPoseをtagToVertexMapのみ使用するシンプルな実装に変更
- 不要なTransform2d/Translation2d/Rotation2d計算（standoff）を削除
- Pose sourceのenum切り替え（kDrivePoseSource）を削除し、fused/odomの動的判定ロジックに統一
- nullチェックを強化（fused/odom両方のearly return）
- fusedStepAcceptedの異常dt時にfalseを返すよう安全性を向上
- ログプレフィックスの誤り（[SetZero] → [SetToTag]）を修正
- 未使用のDrivePoseSource enum / kDrivePoseSource定数を削除

## Changed Files
- `src/main/java/frc/robot/commands/debug/odmetry/SetToTagCommand.java` - Refactored版ロジックへの統一、ログプレフィックス修正
- `src/main/java/frc/robot/lib/constants/commandconstants/SetToTagCommandConstants.java` - 未使用のDrivePoseSource enum/kDrivePoseSource削除
- `src/main/java/frc/robot/commands/debug/odmetry/SetToTagCommandRefactored.java` - 削除（git rm）

## Review Status
- Reviewed by: reviewer
- Date: 2026-02-21
- Result: APPROVED
