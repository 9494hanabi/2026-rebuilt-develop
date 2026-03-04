# FRC-2025-Public (Team 254) 機能分析

Team 254 (The Cheesy Poofs) の FRC 2025 Reefscape シーズン公開コードの機能一覧。

---

## 1. 走行系 (Drive)

### Swerve Drive
- **CTRE Phoenix 6 ベースのスワーブ駆動** (`CommandSwerveDrivetrain`)
- **3種類のTuner設定**: Competition / Practice / Simulation (`CompTunerConstants`, `PracTunerConstants`, `SimTunerConstants`)
- **IO抽象化**: `DriveIO` interface + `DriveIOHardware` (実機) / `DriveIOSim` (シミュレーション)
- **AdvantageKit ログ対応**: `DriveViz` でテレメトリ可視化

### ヘディング維持走行 (`DriveMaintainingHeadingCommand`)
- ジョイスティック操作がないとき、最後のヘディングを自動維持
- **コーラルモード**: Reef最近面へのスナップヘディング（ヒステリシス付き）
- **バージモード**: フィールド端方向への自動ヘディングロック
- **プロセッサモード**: プロセッサ方向への自動ヘディングロック
- Field-Centric + Velocity制御 / シミュレーション時はOpenLoopVoltage

### 自動位置合わせ (`AutoAlignToPoseCommand`)
- ProfiledPIDController による並進 + 回転の同時制御
- フィードフォワード付きトラペゾイドプロファイル
- 位置許容誤差: 0.04m, 角度許容誤差: 2度

### PathPlanning自動位置合わせ (`PathfindingAutoAlignCommand`)
- PathPlannerのパスファインディング → Join Path → スコアリングの一連を統合
- **速度制約の動的切替**: エレベーター高時は減速（`slowPathfindConstraints`）
- **スコアリング準備条件**: CTE, 角度, Y速度, θ速度, ロール/ピッチ速度, ビジョン鮮度/精度
- **ステージングタイミング制御**: 距離ベースでEventMarkerを動的配置
- **ウォームアップ機構**: `WarmupCommand` でパスファインディングを事前計算
- Reef / Feeder / Barge / アルゲー対応の分岐ロジック

---

## 2. ビジョン (Vision)

### VisionSubsystem
- **IO抽象化**: `VisionIO` interface + `VisionIOHardwareLimelight` (実機) / `VisionIOSimPhoton` (シミュレーション)
- **Megatag Pose推定**: `MegatagPoseEstimate` でAprilTagベースの位置推定
- **フィールドポーズ推定**: `VisionFieldPoseEstimate` をRobotState経由でDriveSubsystemへフィードバック
- **Fiducial観測データ**: `FiducialObservation` で個別タグ検出
- **JSON設定適用**: `VisionJsonApplierLimelight` でLimelight設定を動的に適用
- **排他タグ制御**: スコアリング中に特定タグIDを優先して観測

---

## 3. スーパーストラクチャ (Superstructure)

### 状態機械 (`SuperstructureStateMachine`)
- **A*探索による安全遷移経路の自動選択**
- 全状態ペアのパスを起動時にプリコンピュート
- 遷移コストファイル (`transition_costs.txt`) からの実測値読み込み
- **動的遷移ブロック**: アルゲー保持中はコーラル状態への遷移をブロック
- **キャラクタリゼーションコマンド**: 全遷移を自動実行して遷移時間を計測・ファイル出力
- **Future Desired State**: 条件未達時に目標状態を3秒間バッファリング

### SuperstructureState (全24状態)
| カテゴリ | 状態 |
|---------|------|
| Stow系 | `STOW_CORAL`, `STOW_ALGAE`, `STOW_ELEVATOR` |
| クリアランス系 | `CLEAR_ELEVATOR_CROSSBEAM`, `CLEAR_WHEELS`, `CLEAR_LIMELIGHT`, `CLEAR_BUMPER` |
| インテーク系 | `GROUND_ALGAE_INTAKE`, `LOLLIPOP_INTAKE`, `REEF_ALGAE_SLAP`, `REEF_ALGAE_INTAKE_L2/L3/L3_MANUAL` |
| コーラルスコア系 | `STAGE_CORAL_L1/L2/L3/L4`, `NON_DESCORE_CORAL_L2/L3/L4`, `INTERMEDIATE_CORAL_L4` |
| アルゲースコア系 | `STAGE_PROCESSOR`, `INTERMEDIATE_BARGE`, `STAGE_BARGE` |

### Reef最近面検出
- 6面のReef面との距離を速度予測付きで計算
- ヒステリシス（5%）付きで頻繁な切替を防止

---

## 4. メカニズム系サブシステム

### Elevator (`ElevatorSubsystem`)
- **リード + フォロワ構成** (`ServoMotorSubsystemWithFollowers`)
- **MotionMagicプロファイル**: 上下で異なるパラメータ
- **CANrange**: 安全格納距離の判定
- **シミュレーション対応**: `SimElevator` でリアルな物理シミュレーション

### Wrist (`WristSubsystem`)
- **CANCoderフィードバック** (`ServoMotorSubsystemWithCanCoder`)
- アルゲー保持時のゲイン切替
- エレベーター干渉回避の安全制御

### Claw (`ClawSubsystem`)
- **コーラル/アルゲー兼用**
- **アルゲー検出**: 電流/速度の条件で自動検出
- **トルク電流制御**: 保持/吸い込み/排出モード
- `clearOfIndexer()` でWrist干渉回避
- バナーセンサ: ステージ/スコア検出

### Intake Pivot (`IntakePivotSubsystem`)
- **CANCoderフィードバック** (`ServoMotorSubsystemWithCanCoder`)
- MotionMagicで展開/格納を制御
- `hasCoralAtIntake()`: 完全展開 + 位置閾値でインテーク到達判定
- Auto/Teleopで異なるデフォルトコマンド

### Intake Roller (`IntakeRollerSubsystem`)
- 回転数/RPSをRobotStateに反映

### Indexer (`IndexerSubsystem`)
- **1st/2ndバナーセンサ** でデバウンス
- `CoralStateTracker` を更新

### CoralStateTracker
- 状態遷移: `NONE` → `AT_FIRST_INDEXER` → `GOING_TO_SECOND_INDEXER` → `AT_SECOND_INDEXER` → `PROCESSING_IN_CLAW` → `STAGED_IN_CLAW`
- タイムアウト: 0.5s / 1.5s

### Climber Pivot (`ClimberPivotSubsystem`)
- **CANCoderフィードバック**
- 電圧駆動で展開→巻き上げ

### Climber Roller (`ClimberRollerSubsystem`)
- **リミットスイッチ**: 両側でラッチ完了を検知

### LED (`LedSubsystem`)
- `LedIO` + `LedIOHardware`
- `LedState` でRGB状態を管理

---

## 5. ファクトリ (Factories)

コマンドをサブシステム横断で構築するファクトリパターン。

| ファクトリ | 役割 |
|-----------|------|
| `SuperstructureFactory` | Elevator + Wrist + Claw + Intake の協調制御コマンド |
| `AutoFactory` | Pathfinding + Auto Align + Feeder の自動コマンド |
| `IntakeFactory` | Intake Pivot + Roller の展開/格納コマンド |
| `IndexerFactory` | Indexer の排出コマンド |
| `ClawFactory` | Claw のステージ/スコアコマンド |
| `ElevatorFactory` | Elevator の高さ制御コマンド |
| `WristFactory` | Wrist の角度制御コマンド（アルゲーゲイン切替含む） |
| `ClimberFactory` | Climber の展開→巻き上げ自動シーケンス |
| `LedFactory` | LED状態コマンド |

---

## 6. 自律走行 (Auto)

### AutoModeSelector
- **Dashboard Chooser**: モード / 開始位置 / アイスクリーム数 / フィーダー戦略
- **カスタム自律**: scoreOrder + levelOrder の文字列ベース指定
  - 例: `scoreOrder = "IKLLKL"`, `levelOrder = "44433*"` → I(L4), K(L4), L(L4), L(L3), K(L3), L(アルゲー)
- **開始位置**: LEFT_BARGE / MIDDLE_BARGE / RIGHT_BARGE
- **フィーダー戦略**: FUNNEL / GROUND
- L2 / L3 / L4 / アルゲー の全12位置×3レベル + 6面アルゲー

### PathfindingAuto
- `ChezyRepeatCommand` で動的にステップを進行
- **パスファインディング → Reefスコアリング → フィーダー/アイスクリーム回収の繰り返し**
- アルゲーインテーク: Slapパターン + Backoff + Ground Intake
- アイスクリーム回収: 最近距離の未回収アイスクリームを選択
- **時間制約付き判断**: 12秒以降はL3/L2をスキップしてアルゲーへ
- リトライ機構: コーラル未保持時にステップを巻き戻し

### PathfindingWarmupCommand
- Disabled中にパスファインディングをプリコンピュート

---

## 7. コントローラー操作 (Control Board)

### ControlBoard
- **ドライバー + オペレーター** の2コントローラー構成
- `IDriveControlBoard`: スロットル/ストレイフ/ローテーション入力
- `IButtonControlBoard`: ボタン入力抽象化
- `GamepadDriveControlBoard` / `GamepadButtonControlBoard`: Gamepad実装

### ModalControls
- **モード切替**: CORAL / ALGAECLIMB
- `coralMode()`, `algaeClimbMode()`, `coralManualMode()` の判定

### ModalSuperstructureTriggers
- モードに応じたボタン→状態遷移のトリガーマッピング
- SnapHeading有効/無効の制御
- Auto/Teleop対応

---

## 8. RobotState (状態統合)

- **走行状態**: フィールド座標系ポーズ履歴(1秒バッファ), ロボット/フィールド速度(実測/目標/融合)
- **角速度/姿勢**: Yaw/Pitch/Roll角速度, Pitch/Roll角度, X/Y加速度の履歴
- **機構状態**: Elevator高さ, Wrist角度, Claw/Intake/Indexer/Climber回転数/RPS
- **ポーズ予測**: `getPredictedFieldToRobot()` で速度ベースの先読み
- **ビジョン統合**: Megatag推定のタイムスタンプ/ポーズを記録
- **LED状態**, **排他タグ**, **軌道目標/現在ポーズ**, **自動パスキャンセルフラグ**
- **アライアンス/相手側判定**
- **AdvantageKitへの集約ログ出力**

---

## 9. ライブラリ (`com.team254.lib`)

### Subsystem基盤
| クラス | 役割 |
|--------|------|
| `ServoMotorSubsystem` | TalonFXベースの位置/速度制御サブシステム基盤 |
| `ServoMotorSubsystemWithCanCoder` | + CANCoderフィードバック |
| `ServoMotorSubsystemWithFollowers` | + フォロワモータ |
| `TalonFXIO` / `SimTalonFXIO` | モータIO抽象 |
| `CanCoderIO` / `CanCoderIOHardware` / `SimCanCoderIO` | CANCoder IO抽象 |
| `SimElevator` | エレベーター物理シミュレーション |

### PathPlanner (カスタムフォーク)
- `AutoBuilder`: パスファインディングとパス追従の統合ビルダー
- `FollowPathCommand` / `PathfindingCommand`: パス追従/パスファインディングコマンド
- `PPHolonomicDriveController` / `PPLTVController`: ホロノミック/LTV制御器
- `LocalADStar`: AD*パスファインディングアルゴリズム
- `SwerveSetpointGenerator`: スワーブのセットポイント生成
- `IPathCallback`: **パス走行中にConstraintZoneやEventMarkerを動的に差し替え可能**
- `FlippingUtil`: アライアンス反転ユーティリティ

### Reefscape
- `ReefBranch`: A-L + アルゲー面の定義（AprilTag ID対応）
- `ScoringLocation`: 全スコアリング位置の座標定義（コーラルL1-L4, アルゲー, フィーダー, バージ, アイスクリーム）

### ユーティリティ
| クラス | 役割 |
|--------|------|
| `ConcurrentTimeInterpolatableBuffer` | スレッドセーフな時系列補間バッファ |
| `CurrentSpikeDetector` | 電流スパイク検出 |
| `CustomProfiledPIDController` | カスタムProfiledPID |
| `ExponentialMovingAverage/Pose` | 指数移動平均 |
| `CheesyTrigger` | カスタムトリガー |
| `LatchedBoolean` | ラッチ付きBoolean |
| `MathHelpers` | 数学ヘルパ |
| `FieldConstants` | フィールド座標定数 (Reef中心, 面, 高さ等) |
| `ControllerMapping/Mappings` | コントローラーマッピング |
| `CANBusStatusLogger` / `CANStatusLogger` | CANバス状態ログ |
| `ChezyRepeatCommand` / `ChezySequenceCommandGroup` | カスタムコマンドグループ |

---

## 10. 可視化 (Visualization)

- `RobotViz`: AdvantageScope向けの機構ビジュアライズ
- `ReefViz`: Reef状態の可視化
- `DriveViz`: 走行状態の可視化

---

## 11. シミュレーション

- `SimulatedRobotState`: シミュレーション用の状態管理
- `MapleSimSwerveDrivetrain`: MapleSim連携のスワーブシミュレーション
- 全サブシステムに Sim IO 実装:
  - `DriveIOSim`, `VisionIOSimPhoton`, `ClawSensorIOSim`, `ClimberSensorIOSim`
  - `ElevatorSensorIOSim`, `IndexerSensorIOSim`, `SimTalonFXIO`, `SimCanCoderIO`, `SimElevator`

---

## 12. ビルド・依存関係

| 項目 | 値 |
|------|-----|
| GradleRIO | 2025.3.2 |
| Java | 17 |
| フォーマッタ | Spotless (Google Java Format AOSP) |
| ログ | AdvantageKit |
| テスト | JUnit 5 |
| JVM | SerialGC, 100MB heap, AlwaysPreTouch |
| バージョン管理 | gversion (BuildConstants自動生成) |

---

## 13. 技術的な特徴まとめ

1. **A*探索による安全な機構遷移** - 24状態間の全経路をプリコンピュート + 実測コストでの最適経路選択
2. **動的パス制約** - エレベーター高さ/ステージング状態に応じたリアルタイム制約変更
3. **IPathCallbackによる動的パスマークアップ** - 走行中にConstraintZoneやEventMarkerを差し替え
4. **文字列ベースの柔軟な自律構成** - scoreOrder + levelOrder で任意の自律シーケンスを組み合わせ
5. **包括的なシミュレーション** - 全サブシステムにSim IO、MapleSim連携、PhotonVision連携
6. **モーダルコントロール** - CORAL/ALGAECLIMBモードに応じたボタンリマッピングとヘディングスナップ
7. **コーラル搬送のセンサ駆動状態追跡** - バナーセンサ + デバウンス + タイムアウトの状態機械
8. **遷移時間キャラクタリゼーション** - 全遷移を自動実行して実測時間を記録→A*コストに反映
