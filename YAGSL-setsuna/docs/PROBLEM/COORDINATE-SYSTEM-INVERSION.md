## Symptoms
- テレオペでは正しく動作するが、SetToTagCommand/SetZeroCommand（半自動制御）で座標系が反転
- タグ1,2（リアル左上 6.05, 9.00）を観測 → ロボットがリアル右下へ移動
- タグ3,4（リアル左上 0, 9.00）を観測 → ロボットがリアル左下へ移動
- SetZeroCommandのログ: リアル(6.05, 9.00)付近で (0.39, 1.22, 32deg) と報告
- Fused拒否（odomのみ）でも発生 → Limelightが原因ではない
- SetDriveHorizonCommand に「+vxが体感後進」の注記あり

## 確定事項
1. テレオペ操作は正常（前進スティック → 物理+X方向）
2. DriverStation同盟色は未設定 → `allianceRelativeControl(true)` は無効
3. 起動時ロボット正面は+X方向 → `kInitialFieldToRobotPose` の0°と一致
4. 反転は再現性あり、実機の移動方向と推定座標系が一致していない
5. `odomlog-0.json` では `yagsl / fused / odom` がほぼ同じ値 → 選択ロジック後段ではなく入口側でズレ
6. `OdomTranslationController` / `OdomHeadingController` の基本ロジック不具合ではない（単体テスト通過）
7. fmap変換・タグ座標定数の単純な変換ミスではない（単体テスト通過）

## 除外された仮説

### 仮説1: `invertedIMU: false` が誤り（IMUヨー反転）
- **根拠**: NavXはCW正、WPILibはCCW正を期待するため、反転が必要に見える
- **除外理由**: 同盟色未設定で `allianceRelativeControl` が無効にもかかわらずテレオペが正常。
  ヘディングが反転していれば `driveFieldOriented` の並進変換も壊れるため、
  テレオペの前進が正しく+Xになることと矛盾する。
  `invertedIMU: true` にするとテレオペ側が崩れる可能性が高い。

### 仮説2: SetToTagの並進計算実装単体の問題
- **除外理由**: `OdomTranslationController` 統一後も現象継続

### 仮説3: fmap変換・タグ座標定数の変換ミス
- **除外理由**: 単体テストで主要整合を確認済み

## 最有力仮説: ドライブエンコーダの距離符号が逆

### メカニズム
```
物理的に前進 → モーターは正方向に回転 → テレオペ正常 ✓
                       ↓
                 エンコーダ値が減少（符号が逆）
                       ↓
                 odometry.update() が「後退した」と解釈
                       ↓
                 オドメトリ位置が反転
```

- テレオペはモーター電圧/速度指令で動くためエンコーダ値に依存しない → 正常
- オドメトリはエンコーダの変化量から位置を計算 → 符号が逆なら位置が反転
- YAGSL/fused/odom全てが同じエンコーダデータを使用 → 全て同じ方向に反転

### 全症状との整合
| 症状 | エンコーダ符号反転で説明 |
|---|---|
| テレオペ正常 | モーター制御はエンコーダ非依存 |
| オドメトリ位置反転 | 距離が逆符号 → 積算方向が逆 |
| `kOdomOmegaSign = -1.0` が必要 | odom heading変化方向が不整合のため補正 |
| YAGSL/fused/odom全て同値 | 全て同じエンコーダデータを使用 |
| `+vxが体感後進` (SetDriveHorizon) | odomベースの判断が反転 |
| 同盟色に依存しない | エンコーダは同盟設定と無関係 |

## 診断手順

### Step 1: エンコーダ符号の確認
`SwerveSubsystem.periodic()` に以下のログを追加:
```java
SwerveModulePosition[] positions = swerveDrive.getModulePositions();
System.out.printf("[OdomDebug] FL=%.3f FR=%.3f BL=%.3f BR=%.3f yaw=%.1f%n",
    positions[0].distanceMeters,
    positions[1].distanceMeters,
    positions[2].distanceMeters,
    positions[3].distanceMeters,
    swerveDrive.getYaw().getDegrees());
```
**ロボットを手で+X方向に押して、distanceMetersが増加するか減少するかを確認。**
- 減少 → エンコーダ符号が原因（この仮説が正）
- 増加 → 別の原因を調査

### Step 2: ヨー符号の確認
同じログで、ロボットをCCW方向に手で回し、`yaw` が増加するか確認。
- 減少 → IMU符号も問題あり（`invertedIMU` も要修正）
- 増加 → IMU符号は正常

### Step 3: 修正適用
エンコーダ符号が逆の場合、各モジュールJSONを修正:
```json
"inverted": {
    "drive": true,
    "angle": false
}
```
**注意**: `drive.inverted` はモーター指令も反転するため、テレオペの前後も反転する。
`RobotContainer` のジョイスティック `* -1` を `* 1` に変更して再調整が必要。

### Step 4: 補正定数の見直し
エンコーダ修正後、以下の対症療法的定数をデフォルトに戻す:
- `OdomConstants.kOdomOmegaSign`: `-1.0` → `+1.0`
- `SetDriveHorizonCommand.kForwardSpeedMetersPerSec`: `-1.0` → `+1.0`
- テレオペ動作テスト後にジョイスティック符号を調整

## Affected Files
- `src/main/deploy/swerve/modules/frontleft.json` — `drive.inverted`
- `src/main/deploy/swerve/modules/frontright.json` — `drive.inverted`
- `src/main/deploy/swerve/modules/backleft.json` — `drive.inverted`
- `src/main/deploy/swerve/modules/backright.json` — `drive.inverted`
- `src/main/deploy/swerve/swervedrive.json` — `invertedIMU`（Step 2の結果次第）
- `src/main/java/frc/robot/lib/constants/OdomConstants.java` — `kOdomOmegaSign`
- `src/main/java/frc/robot/commands/debug/odmetry/SetDriveHorizonCommand.java` — `kForwardSpeedMetersPerSec`
- `src/main/java/frc/robot/RobotContainer.java` — joystick符号

## Prevention
- キャリブレーション手順にエンコーダ符号テストを必須項目として追加
- 起動直後に「手押しで+X → distanceMeters増加」を確認するチェックリスト
- 座標系の整合性テスト（odom delta の符号 vs 物理移動方向）
- IMU反転フラグのチェック項目追加

## Metadata
- Reported by: user
- Documented by: lead
- Initial report: 2026-02-21
- 仮説1（invertedIMU）除外: 2026-02-21
- 最有力仮説（エンコーダ符号）提示: 2026-02-21
- Status: 診断Step 1待ち（実機テスト必要）
