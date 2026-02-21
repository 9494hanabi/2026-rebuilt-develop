# Coding Standards - YAGSL-Setsuna

## 1. Naming Conventions

### Classes
- **Subsystems**: `*Subsystem` (e.g., `SwerveSubsystem`, `TurretSubsystem`)
- **Commands**: `*Command` (e.g., `DriveControllCommand`, `SetToTagCommand`)
- **I/O Interfaces**: `*IO` (e.g., `VisionIO`)
- **I/O Hardware Impl**: `*IOHardware*` (e.g., `VisionIOHardwareLimelight`)
- **I/O Dummy Impl**: `*IODummy` (e.g., `VisionIODummy`)
- **Constants**: `*Constants` (e.g., `PIDConstants`, `VisionConstants`)
- **Utilities**: `*Helpers`, `*Service`, `*Controller` (e.g., `MathHelpers`)
- **Data Records**: Descriptive name (e.g., `FiducialObservation`, `ControlResult`)

### Variables
- **Static constants**: `k` prefix + PascalCase (e.g., `kMaxSpeed`, `kHeadingKp`)
- **Instance members**: `m_` prefix + camelCase (e.g., `m_robotContainer`, `m_chooser`)
- **Local variables**: camelCase (e.g., `currentX`, `targetHeading`)
- **Booleans**: `is*`, `has*`, `at*` prefix (e.g., `isFinitePose`, `atSetpoint`)
- **Unit suffixes**: Always append units (e.g., `kHeadingToleranceRad`, `kMaxSpeedMPerSec`, `kPitchDegrees`)

### Methods
- **Getters**: `get*()` (e.g., `getSwerveDrive()`, `getLatestFieldToRobot()`)
- **Setters**: `set*()` (e.g., `setDefaultCommand()`, `setExclusiveTag()`)
- **Queries**: `is*()`, `has*()`, `at*()` (e.g., `isRedAlliance()`, `atGoal()`)
- **Calculations**: `calculate*()` (e.g., `calculateHeadingPID()`)

## 2. Architecture Patterns

### Command-Based (WPILib)
- All robot actions are `Command` subclasses
- Override `initialize()`, `execute()`, `isFinished()`, `end(boolean interrupted)`
- Commands receive subsystem dependencies via constructor injection

### Subsystem I/O Abstraction
- Define `*IO` interface for hardware abstraction
- `*IOHardware*` for real hardware, `*IODummy` for simulation
- Subsystem uses only the interface, never the concrete class

### Bindings Pattern
- Separate binding classes by domain: `DriveBindings`, `AutoBindings`, `DebugBindings`
- Each has a `configure()` method called from `RobotContainer`
- Controller button mappings go here, NOT in subsystems or commands

### RobotState (Global State)
- Central `RobotState` class holds pose history, velocities, sensor data
- Uses `ConcurrentTimeInterpolatableBuffer` for time-series data
- Vision fusion integrates through callbacks

### Constants Organization
- One constants class per subsystem/domain in `lib/constants/`
- Use `static final` fields with `k` prefix
- Import constants with `static import` where appropriate

## 3. Safety / Validation Rules

### Finite Value Checks
All values from sensors or calculations MUST be validated before use:
```java
if (!Double.isFinite(value)) {
    System.out.println("[ModuleName] !!INVALID!! value=" + value);
    // Safe fallback (e.g., stop motors)
    return;
}
```

### Motor Safety
- Always provide a safe fallback when inputs are invalid (e.g., `new ChassisSpeeds()` = stop)
- Emergency stop commands must be accessible from debug bindings
- Clamp motor outputs to safe ranges

### Null / Optional Safety
- Use `Optional<T>` for values that may not exist
- Check `.isEmpty()` before `.get()`
- Never ignore `Optional` results

## 4. Logging Conventions

### Rate-Limited Console Logging
```java
private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

if (nowSec - lastStatusLogSec >= LogConstants.kStatusLogPeriodSec) {
    lastStatusLogSec = nowSec;
    System.out.println("[ClassName] message");
}
```

### Log Format
- Prefix with `[ClassName]` or `[ModuleName]`
- Use `!!WARN!!` or `!!INVALID!!` for error/warning messages
- Include relevant numeric values for debugging

### AdvantageKit Logging
```java
Logger.recordOutput("Subsystem/ValueName", value);
```

### SmartDashboard (Rate-Limited)
```java
if (ts - lastDashboardUpdateSec >= DASHBOARD_UPDATE_PERIOD_SEC) {
    SmartDashboard.putNumber("Label", value);
}
```

## 5. Code Review Checklist

The reviewer MUST verify all of the following:

1. **Naming**: Follows `k` prefix for constants, `m_` for members, unit suffixes on numeric values
2. **File Ownership**: Changes are within the implementer's owned directories (see CLAUDE.md)
3. **Finite Checks**: All sensor/calculation values validated with `Double.isFinite()` before use
4. **Safe Fallbacks**: Invalid inputs result in safe motor stops, not crashes
5. **Logging**: Rate-limited, prefixed with `[ClassName]`, includes relevant values
6. **Constants**: No magic numbers; all tuning values in appropriate `*Constants` class
7. **Architecture**: Commands use constructor injection, I/O abstraction where applicable
8. **Optional Handling**: `Optional` values checked before access, no raw `.get()` without guard
9. **Unit Consistency**: Variable names include units (Rad, Deg, MPerSec, etc.), conversions are correct
10. **No Side Effects**: Shared state (RobotState) accessed correctly, no unintended mutations
