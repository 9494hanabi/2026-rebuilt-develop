# Requirements - YAGSL-Setsuna

## 1. System Overview

YAGSL-Setsunaは、FRC (FIRST Robotics Competition) ロボット制御ソフトウェアです。
WPILib Command-Based フレームワークと YAGSL (Yet Another Swerve Drive Library) を使用した
スワーブドライブロボットの制御を行います。

### Key Technologies
- **WPILib**: FRC標準ロボットフレームワーク
- **YAGSL**: スワーブドライブ抽象化ライブラリ
- **PathPlanner**: 自律パスプランニング
- **AdvantageKit**: テレメトリ・ロギング
- **Limelight**: AprilTagビジョンシステム

## 2. Architecture

```
┌─────────────────────────────────────────────────┐
│                  RobotContainer                  │
│  (Dependency Injection & Command Binding Hub)    │
├──────────┬──────────┬───────────┬───────────────┤
│ Drive    │ Auto     │ Debug     │ Modal         │
│ Bindings │ Bindings │ Bindings  │ Controls      │
├──────────┴──────────┴───────────┴───────────────┤
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │ Swerve       │  │ Vision       │             │
│  │ Subsystem    │  │ Subsystem    │             │
│  │ (YAGSL)      │  │ (IO Pattern) │             │
│  └──────┬───────┘  └──────┬───────┘             │
│         │                  │                     │
│         ▼                  ▼                     │
│  ┌──────────────────────────────┐               │
│  │        RobotState            │               │
│  │  (Pose, Velocity, Sensors)   │               │
│  └──────────────────────────────┘               │
│                                                  │
│  ┌──────────────┐  ┌──────────────┐             │
│  │ Turret       │  │ (Future      │             │
│  │ Subsystem    │  │  Mechanisms) │             │
│  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────┘
```

## 3. Module Requirements

### 3.1 Drive/Vision (impl-drivevision)

#### Swerve Drive
- YAGSL ベースのスワーブドライブ制御
- フィールド指向運転 (Field-Oriented Drive)
- オドメトリ追跡とポーズ推定
- SmartDashboard によるテレメトリ出力

#### Vision System
- VisionIO インターフェースによる抽象化
- Limelight を使用した AprilTag 検出
- MegaTag ポーズ推定 (v1/v2)
- マルチカメラサポート
- オドメトリとの融合（分散ベース重み付け）
- 信頼性チェック（ambiguity、距離しきい値）

#### Odometry Debug Commands
- DriveControllCommand: PID ベースのドライブ制御
- SetToTagCommand: ビジョンベースのターゲット追従
- SetDriveHorizonCommand: ドライブ水平校正
- SetThetaZeroCommand: 角度オフセット校正

### 3.2 Mechanism (impl-mechanism)

#### Turret Subsystem
- ProfiledPIDController によるモーションプロファイル制御
- 角度クランプ (min/max)
- フィードフォワード補償 (kS, kV, kA)
- シミュレーションサポート (DCMotorSim)

#### Future Mechanisms
- 新しいメカニズムサブシステムの追加
- I/O 抽象化パターンの適用
- シミュレーション対応

### 3.3 Integration (impl-integration)

#### Autonomous
- PathPlanner による自律パス実行
- NamedCommands による自律アクション登録
- AutoBuilder 設定と自律選択

#### Bindings Coordination
- AutoBindings: 自律コマンドのバインディング
- DebugBindings: デバッグモード用バインディング
- 安全コマンド (ClearOdometryAndLockStopCommand)

#### Cross-Module Integration
- RobotContainer のサブシステム配線確認
- モジュール間依存関係の整合性テスト
- 全体動作の統合検証

## 4. Non-Functional Requirements

### Safety
- すべてのセンサー値は `Double.isFinite()` で検証すること
- 無効な入力時は安全なフォールバック（モーター停止）を行うこと
- 緊急停止コマンドがデバッグバインディングからアクセス可能であること

### Performance
- ログ出力はレート制限すること (LogConstants 参照)
- SmartDashboard 更新は周期制限すること
- テレメトリはロボットループのタイミングに影響しないこと

### Testability
- サブシステムは I/O 抽象化パターンを使用すること
- シミュレーション用 Dummy 実装を提供すること
- `RobotBase.isSimulation()` で実機/シミュレーションを切り替え可能にすること

## 5. File Structure & Ownership

```
src/main/java/frc/robot/
├── Robot.java, RobotContainer.java, RobotState.java  [SHARED]
├── bindings/
│   ├── DriveBindings.java          [impl-drivevision]
│   ├── AutoBindings.java           [impl-integration]
│   └── DebugBindings.java          [impl-integration]
├── commands/
│   ├── AutoCommand.java            [impl-integration]
│   └── debug/
│       ├── odmetry/                [impl-drivevision]
│       ├── safety/                 [impl-integration]
│       └── vision/                 [impl-drivevision]
├── lib/
│   ├── constants/
│   │   ├── Constants.java          [SHARED]
│   │   ├── FieldConstants.java     [SHARED]
│   │   ├── LogConstants.java       [SHARED]
│   │   ├── OdomConstants.java      [impl-drivevision]
│   │   ├── PIDConstants.java       [impl-drivevision]
│   │   ├── SemiAutoConstants.java  [impl-integration]
│   │   ├── VisionConstants.java    [impl-drivevision]
│   │   └── commandconstants/       [impl-drivevision]
│   ├── limelight/                  [impl-drivevision]
│   ├── time/                       [SHARED]
│   └── util/                       [SHARED / impl-drivevision for Odom*]
└── subsystems/
    ├── SwerveSubsystem.java        [impl-drivevision]
    ├── TurretSubsystem.java        [impl-mechanism]
    └── vision/                     [impl-drivevision]
```
