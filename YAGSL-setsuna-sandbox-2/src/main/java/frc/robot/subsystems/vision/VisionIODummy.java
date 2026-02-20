package frc.robot.subsystems.vision;

// === 担当 ===
// ひなた
//
// Limelight未搭載のロボット用のダミー実装

public class VisionIODummy implements VisionIO {

    @Override
    public void readInputs(VisionIOInputs inputs) {
        inputs.cameras.clear();
    }
}
