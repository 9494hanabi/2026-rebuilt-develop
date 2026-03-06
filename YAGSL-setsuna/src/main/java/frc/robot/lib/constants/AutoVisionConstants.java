package frc.robot.lib.constants;

/*担当
 * 晴太
 */
// Auto向けVision制御の定数をまとめるクラス。
// ここを見ると「どのカメラを使うか」「どのpipelineを使うか」「補正の強さ」が分かる。
public final class AutoVisionConstants {
  private AutoVisionConstants() {}

    // 以下AprilTag cameraの定数
  public static final String kVisionTableName = VisionConstants.kLimelightBTableName; // front-center の AprilTag 用 Limelight table 名
  public static final int kAprilTagPipeline = 1; // AprilTag 位置合わせ用 pipeline

  // 以下PieceVision cameraの定数
  public static final String kPieceCameraTableName = "limelight_intake"; // intake 用 LL4 の NetworkTables 名
  public static final int kPieceDetectorPipelineIndex = 2; // CPU detector を動かす pipeline
  public static final int kPieceIdlePipelineIndex = 1; // piece detector を使わない時に戻す pipeline


  // Tag align
  public static final double kTagAlignTimeoutSec = 1.0; // 安全タイムアウト（到達できない時の保険）
  public static final double kTagAlignDistanceToleranceMeter = 0.08; // 到達判定距離[m]


  // Autoで狙うタグID（今回はTag 7固定運用）。
  public static final int kDefaultScoreTagId = 7; 
  
  // 到達後にこの時間だけ安定していれば「到達完了」とみなす
  public static final double kTagAlignSettleSec = 0.10;

  // SetToTagCommandConstantsの基準姿勢に対する追加角度オフセット[deg]
  public static final double kTagAlignHeadingOffsetDeg = 45;

  // 以下PieceVision detector filterの定数
  public static final int kPieceTargetClassId = -1; // Fuel の class id。確定までは class filter を無効化
  public static final double kMinPieceDetectionArea = 0.05; // 小さすぎる誤検出を捨てる最小面積
  public static final double kMaxPieceAbsTxDeg = 27.0; // 画角端の不安定な検出を除外する最大横ずれ[deg]
  public static final double kPieceTargetStaleSec = 0.20; // 古い last seen を無効とみなす時間[s]

  // 以下PieceVision cluster scoreの定数
  public static final double kPieceClusterMergeTxDeg = 4.5; // 同じ塊とみなす横方向の許容差[deg]
  public static final double kPieceClusterCountWeight = 0.20; // 個数が多い塊を優先する重み
  public static final double kPieceClusterWidthPenalty = 0.02; // 横に広がりすぎた塊を少し不利にする重み



  // 以下piece auto intakeの定数
  public static final double kPieceForwardSpeedMps = 0.70; // Fuel 塊へ向かう時の基準前進速度[m/s]
  public static final double kPieceTurnGainRadPerSecPerDeg = 0.04; // 横ずれ[deg]を旋回速度へ変える比例ゲイン
  public static final double kPieceMaxTurnRateRadPerSec = 1.20; // 旋回しすぎを防ぐ最大角速度[rad/s]
  public static final double kPieceCenteringDeadbandDeg = 3.0; // ほぼ中央とみなす横ずれの範囲[deg]
  public static final double kPieceOffCenterForwardScale = 0.40; // 横にずれている時の前進倍率
  public static final double kPieceLostTargetForwardScale = 0.55; // 見失い直後に少し押し込む時の前進倍率
  public static final double kPieceLastSeenHoldSec = 0.20; // 最後に見えた方向へ追従を続ける時間[s]
  public static final double kPieceCollectStartAreaThreshold = 2.5; // 「十分近づいた」とみなして回収窓を始める面積
  public static final double kPieceNoTargetFallbackDelaySec = 0.35; // target が見えないまま fallback path に切り替えるまでの待ち時間[s]
  public static final double kPieceCollectWindowSec = 0.45; // センサー未接続時に最後に押し込む時間[s]
  public static final double kPieceAcquireTimeoutSec = 5.0; // 最後の保険としての絶対 timeout[s]
  public static final double kPiecePostCaptureContinueSec = 0.20; // 将来 CANrange で取れた後に送り込む時間[s]
  public static final String kPieceFallbackPathName = "back"; // piece が見えない時に抜ける fallback path 名

  /* 以下CANrangeの定数
  * これは ToF センサー導入後に piece capture 判定で使う。
  */
  public static final boolean kPieceCaptureSensorEnabled = false;
  public static final int kPieceEntrySensorCanId = 30; // intake 入口側 CANrange の仮 ID
  public static final int kPieceLatchSensorCanId = 31; // box 側 CANrange の仮 ID
  public static final double kPieceEntryDetectDistanceMeter = 0.14; // intake 入口で検出とみなす距離[m]
  public static final double kPieceLatchDetectDistanceMeter = 0.09; // box 側で検出とみなす距離[m]
  public static final double kPieceSensorHysteresisMeter = 0.01; // ToF のチャタリングを抑えるヒステリシス[m]

}
