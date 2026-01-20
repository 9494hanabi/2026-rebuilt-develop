package frc.robot.lib.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;

// === 担当者 ===
// ひなた
//

public class MathHelpers {
    // ロボットの姿勢(x, y, r)を格納する配列
    public static final Pose2d kPose2dZero = new Pose2d();

    // 回転から姿勢作成
    public static final Pose2d pose2dFromRotation(Rotation2d rotation) {
        return new Pose2d(kTranslation2dZero, rotation);
    }

    // 変位ベクトルから姿勢作成
    public static final Pose2d pose2dFromTranslation(Translation2d translation) {
        return new Pose2d(translation, kRotation2dZero);
    }

    // 角度変位0
    public static final Rotation2d kRotation2dZero = new Rotation2d();

    // 角度変位180°
    public static final Rotation2d kRotation2dPi = Rotation2d.fromDegrees(180.0);

    // 位置変位(0)
    public static final Translation2d kTranslation2dZero = new Translation2d();

    // 回転変位のみから変位ベクトル(並進+回転)を作成
    public static final Transform2d transform2dFromRotation(Rotation2d rotation) {
        return new Transform2d(kTranslation2dZero, rotation);
    }

    // 並進変位のみから変位ベクトルを作成
    public static final Transform2d transform2dFromTranslation(Translation2d translation) {
        return new Transform2d(translation, kRotation2dZero);
    }

    // ロボットが２点間のどこ(何%ドライブしたのか)にいるのか計算するメソッド
    public static double reverseInterpolate(
            Translation2d query, Translation2d start, Translation2d end) {
        
        // vector calculation
        // segment = end - start
        // queryToStart = query - start
        Translation2d segment = end.minus(start);
        Translation2d queryToStart = query.minus(start);

        // vector calculation
        // segmentLengthSqr = x_segment² + y_segment²
        double segmentLengthSqr = segment.getX() * segment.getX() + segment.getY() * segment.getY();

        if (segmentLengthSqr == 0.0) {
            return 0.0;
        }

        // vector calculation
        // queryとsegmentのベクトル内積を計算し、segmentとの類似度を比較できるようにする。
        return (queryToStart.getX() * segment.getX() + queryToStart.getY() * segment.getY())
                / segmentLengthSqr;
    }

    // ロボットがドライブする線分からどれだけ離れているのか計算するメソッド
    public static double distanceToLineSegment(
            Translation2d query, Translation2d start, Translation2d end) {
        double t = reverseInterpolate(query, start, end);
        if (t < 0.0) {
            return query.getDistance(start);
        } else if (t > 1.0) {
            return query.getDistance(end);
        } else  {
            Translation2d segment = end.minus(start);

            // closespoint = start + (segment * t)
            // closespointとの距離を取得する。
            Translation2d closesPoint = start.plus(segment.times(t));
            return query.getDistance(closesPoint);
        }
    }

    // startより
    public static double perpendicularDistanceToLine(
            Translation2d query, Translation2d start, Translation2d end) {
        double t = reverseInterpolate(query, start, end);
        Translation2d segment = end.minus(start);

        // closespoint = start + (segment * t)
        // closespointとの距離を取得する。
        Translation2d closesPoint = start.plus(segment.times(t));
        return query.getDistance(closesPoint);
    }
}
