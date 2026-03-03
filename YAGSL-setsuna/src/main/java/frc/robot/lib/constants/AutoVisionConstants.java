package frc.robot.lib.constants;

/*担当
 * 晴太
 */
// Auto向けVision制御の定数をまとめるクラス。
// ここを見ると「どのカメラを使うか」「どのpipelineを使うか」「補正の強さ」が分かる。
public final class AutoVisionConstants {
  private AutoVisionConstants() {}

  // 現在は front-center 1台運用
  // 将来2台構成にするときは、用途ごとにテーブル名を分ける。
  public static final String kVisionTableName = VisionConstants.kLimelightBTableName;

  // Limelight pipeline番号。
  // 実機WebUI側の設定番号と必ず一致させる。
  public static final int kAprilTagPipeline = 1; // AprilTag認識用（位置合わせ）
  public static final int kPieceDetectorPipeline = 2; // フェルール/ゲームピース認識用（要実機確認）

  // Tag align
  public static final double kTagAlignTimeoutSec = 1.0; // 安全タイムアウト（到達できない時の保険）
  public static final double kTagAlignDistanceToleranceMeter = 0.08; // 到達判定距離[m]


  // Autoで狙うタグID（今回はTag 7固定運用）。
  public static final int kDefaultScoreTagId = 7;
  
  // 到達後にこの時間だけ安定していれば「到達完了」とみなす
  public static final double kTagAlignSettleSec = 0.10;

  // SetToTagCommandConstantsの基準姿勢に対する追加角度オフセット[deg]
  public static final double kTagAlignHeadingOffsetDeg = 45;


  // フェルール回収補助（AutoIntakeAssistCommand）のパラメータ。
  // 最初は保守的な値で開始し、実機ログを見て調整する。
  public static final double kPieceAcquireTimeoutSec = 5; // 全体の安全タイムアウト
  public static final double kPieceForwardMps = 0.70;
  public static final double kPieceTurnKpRadPerSecPerDeg = 0.04;
  public static final double kPieceMaxOmegaRadPerSec = 1.20;
  public static final double kPieceCenterDeadbandDeg = 3.0;

  // 「1m到達」の近似トリガー(まずはtaで運用)
  public static final double kPieceNearStartAreaThreshold = 2.5;

  // 近距離到達後、この秒数だけ回収動作を継続してから終了
  public static final double kPieceCollectWindowSec = 0.45;

}
