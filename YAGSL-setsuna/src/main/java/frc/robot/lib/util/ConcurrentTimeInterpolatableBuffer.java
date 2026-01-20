package frc.robot.lib.util;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.interpolation.Interpolatable;
import edu.wpi.first.math.interpolation.Interpolator;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

//
// 任意の型のセンサ計測値を時系列順に保存するための機能を提供しているクラス。
// 

// === 担当者 ===
// ひなた
//

public class ConcurrentTimeInterpolatableBuffer<T> {

    // m_ はメンバ変数(オブジェクト破棄まで維持される変数)であることを示す。

    private final double m_historySize;
    private final Interpolator<T> m_interpolatingFunc;
    private final ConcurrentNavigableMap<Double, T> m_pastSnapshots = new ConcurrentSkipListMap<>();

    private ConcurrentTimeInterpolatableBuffer(
            Interpolator<T> interpolateFunction, double historySizeSeconds) {
        this.m_historySize = historySizeSeconds;

        // 飛び飛び値の中間値(Buffer)を作るための関数
        this.m_interpolatingFunc = interpolateFunction;
    }


    // バッファ作成 (線形補間)
    public static <T>  ConcurrentTimeInterpolatableBuffer<T> createBuffer(
        Interpolator<T> interpolateFunction, double historySizeSeconds) {
        return new ConcurrentTimeInterpolatableBuffer<>(interpolateFunction, historySizeSeconds);
    }

    public static <T extends Interpolatable<T>> ConcurrentTimeInterpolatableBuffer<T> createBuffer(
        double historySizeSeconds) {
        return new ConcurrentTimeInterpolatableBuffer<>(
                Interpolatable::interpolate, historySizeSeconds);
    }
    

    public static ConcurrentTimeInterpolatableBuffer<Double> createDoubleBuffer(
            double historySizeSeconds) {
        return new ConcurrentTimeInterpolatableBuffer<>(MathUtil::interpolate, historySizeSeconds);
    }






    // サンプル作成
    public void addSample(double timeSeconds, T sample) {
        m_pastSnapshots.put(timeSeconds, sample);

        // 配列サイズを超えたサンプルを削除
        cleanUp(timeSeconds);
    }

    // timeSecondsを超えたサンプルを削除
    public void cleanUp(double time) {
        m_pastSnapshots.headMap(time - m_historySize, false).clear();
    }

    // 指定されたサンプル全消去
    public void clear() {
        m_pastSnapshots.clear();
    }

    public Optional<T> getSample(double timeSeconds) {
        if (m_pastSnapshots.isEmpty()) {
            return Optional.empty();
        }

        //
        // 要求時刻と完全に一致するサンプル。
        var nowEntry = m_pastSnapshots.get(timeSeconds);

        //
        // nowEntryが見つかったら返す。
        if (nowEntry != null) {
            return Optional.of(nowEntry);
        }

        // 要求時刻以前で最も近いサンプル。
        var bottomBound = m_pastSnapshots.floorEntry(timeSeconds);

        // 要求時刻以後で最も近いサンプル。
        var topBound = m_pastSnapshots.ceilingEntry(timeSeconds);

        //
        // topBとbottomBを探して返す。無かったらEmptyを返す。
        if (topBound == null && bottomBound == null) {
            return Optional.empty();
        } else if (topBound == null) {
            return Optional.of(bottomBound.getValue());
        } else if (bottomBound == null) {
            return Optional.of(topBound.getValue());
        } else {
            // topB bottomBどちらも存在するかつ、その間が要求時刻である時、２つの値を線形に補間して返している。
            return Optional.of(
                    m_interpolatingFunc.interpolate(
                            bottomBound.getValue(),
                            topBound.getValue(),
                            (timeSeconds - bottomBound.getKey())
                                    / (topBound.getKey() - bottomBound.getKey())));
        }
    }

    // 最新のサンプルを返す。
    public Entry<Double, T> getLatest() {
        return m_pastSnapshots.lastEntry();
    }

    // バッファのゲッター
    public ConcurrentNavigableMap<Double, T> getInternalBuffer() {
        return m_pastSnapshots;
    }
}
