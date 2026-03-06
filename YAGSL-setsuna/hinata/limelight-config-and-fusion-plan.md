# Limelight 接続管理(JSON)と観測融合の方策

## 目的
開発期間中に Limelight の設置個数や接続先が変動しても、JSON で「使う/使わない・座標・テーブル名」を管理し、稼働しているカメラからの観測を安定して融合する。

## 参考にする 254 (FRC-2025-Public) の実装ポイント
- 入出力分離: `FRC-2025-Public/src/main/java/com/team254/frc2025/subsystems/vision/VisionIO.java`
- Limelight 入力取得: `FRC-2025-Public/src/main/java/com/team254/frc2025/subsystems/vision/VisionIOHardwareLimelight.java`
- 観測受理と融合: `FRC-2025-Public/src/main/java/com/team254/frc2025/subsystems/vision/VisionSubsystem.java`
  - `fuseEstimates(...)` の逆分散重み付け
  - `processCamera(...)` のゲート条件
- 推定結果の取り込み: `FRC-2025-Public/src/main/java/com/team254/frc2025/RobotState.java`
  - `updateMegatagEstimate(...)` → drive への伝播
- 固定パラメータ: `FRC-2025-Public/src/main/java/com/team254/frc2025/Constants.java` の `VisionConstants`

本方針は、上記の構造を「カメラ数可変」に拡張する。

---

## 設計方針（概要）
1. JSON にカメラ一覧を定義し、起動時に読み込む。
2. `VisionIOInputs` を「固定 A/B」から「可変リスト」に変更する。
3. `VisionIOHardwareLimelight` で JSON の一覧分だけ NetworkTables を監視する。
4. `VisionSubsystem` で、受理された複数カメラの観測を時刻整合 + 逆分散重み付けで融合する。
5. 接続判定は「JSON の enabled」と「ネットワーク上の更新有無(heartbeat)」の両方で行う。

---

## JSON 設定案
配置場所例: `src/main/deploy/vision/limelights.json`

```json
{
  "limelights": [
    {
      "name": "front-left",
      "table": "limelight-left",
      "enabled": true,
      "robotToCamera": {
        "x": 0.20,
        "y": -0.30,
        "yawDeg": 0.0
      },
      "cameraPose": {
        "heightMeters": 0.21,
        "pitchDeg": 20.0
      },
      "stdDevScale": 1.0,
      "pipeline": 0
    },
    {
      "name": "front-right",
      "table": "limelight-right",
      "enabled": false,
      "robotToCamera": {
        "x": 0.20,
        "y": 0.30,
        "yawDeg": 0.0
      },
      "cameraPose": {
        "heightMeters": 0.21,
        "pitchDeg": 20.0
      },
      "stdDevScale": 1.0,
      "pipeline": 0
    }
  ],
  "fusion": {
    "maxTimeSkewSec": 0.1,
    "minTags": 1,
    "singleTagYawGateDeg": 5.0
  }
}
```

### JSON の意図
- `enabled`: 配線/設置の有無を人間が明示（JSON 側）
- `table`: Limelight の NT テーブル名 (254 の `VisionConstants.kLimelightATableName` 相当)
- `robotToCamera`: 254 の `kRobotToCamera*` を JSON 化
- `cameraPose`: Limelight 側 `camerapose_robotspace_set` に流す
- `stdDevScale`: カメラ毎の信頼度差（レンズ/設置差）を掛け合わせて調整

---

## 主要クラス構成（提案）
### 1) VisionConfig (新規)
- JSON を読み取り、`List<CameraConfig>` を返す。
- `Filesystem.getDeployDirectory()` + `ObjectMapper` or `Gson` で読み込み。
- `Robot.init()` または `RobotContainer` で一度ロード。

### 2) VisionIOInputs を可変カメラに
- 254 の `VisionIO.VisionIOInputs` は `cameraA/B` 固定。
- 2026 側では `List<CameraInputs>` に変更し、`Map<String, CameraInputs>` で管理。

```java
class VisionIOInputs {
  public List<CameraInputs> cameras = new ArrayList<>();
}
```

### 3) VisionIOHardwareLimelight の拡張
- JSON の `CameraConfig` 分だけ `NetworkTable` を保持。
- 各カメラで `readCameraData(...)` を回す。
- Limelight の存在確認に `hb`(heartbeat) or `tv` の更新時刻を利用。

```text
if (enabled && heartbeatUpdatedWithin(0.5s)) => connected
else => disconnected (観測は無効)
```

### 4) VisionSubsystem の融合拡張
- 254 は 2 台前提で `fuseEstimates(a,b)`。
- 可変台数では、以下のどちらかで統一:
  - (A) 最も信頼度の高い推定を基準に、順に `fuseEstimates` を畳み込み
  - (B) すべての推定を同一時刻に補正してから一括で重み付け平均

A が簡単で 254 ロジック再利用可。

**融合フロー例**
1. 各カメラの `processCamera(...)` で受理判定
2. `accepted` を timestamp で整列
3. 先頭を `fused` にし、残りを `fuseEstimates(fused, next)` で畳み込み

---

## 254 ロジックを活かすポイント
- `VisionSubsystem#processCamera` のゲート条件はそのまま流用
  - タグ枚数、Yaw 差、Z 高さ、ambig など
- `fuseEstimates` の逆分散重み付けを 그대로継承
- `RobotState#updateMegatagEstimate` へ渡す設計は変更しない

---

## 実装ステップ（2026-rebuilt-develop 向け）
1. `vision/limelights.json` を作成（deploy 配下）
2. `VisionConfig` クラス追加
3. `VisionIOInputs` を List 化
4. `VisionIOHardwareLimelight` を JSON 対応に変更
   - `setLLSettings()` をカメラごとに実行
5. `VisionSubsystem` を List カメラ前提で loop するように変更
6. AdvantageKit/SmartDashboard のログに
   - 接続状態, 受理/棄却理由, 融合前後の Pose

---

## 接続判定の実務的ルール
- JSON で `enabled=false` なら常に無視
- `enabled=true` の場合:
  - `hb` が一定時間更新されない（例: 0.5s）→ 接続なし
  - `tv=0` は「接続はあるが見えていない」扱い

---

## 検証方法
- **単体テスト**: JSON を差し替え、1台/2台/0台で起動
- **実機**: 片側 Limelight だけ接続し、もう片方を抜いてログ確認
- **シミュレーション**: 254 の `VisionIOSimPhoton` のパターンに倣い、カメラ数分の仮想入力を与える

---

## まとめ
- 254 の VisionIO + VisionSubsystem 構造はそのまま活かせる。
- A/B 固定を JSON による可変リストへ拡張すれば、開発中の配線変更に強くなる。
- 融合は 254 の逆分散加重 (`fuseEstimates`) を畳み込みで適用するのが最小変更。

