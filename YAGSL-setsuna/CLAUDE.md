# YAGSL-Setsuna

## Team Workflow

### Roles
- **Lead (Manager)**: Requirements, design decisions, communicates with team leader (user). Commits approved changes.
- **reviewer**: Reviews code against requirements and coding standards. Creates docs/CHANGESUMMARY on approval. Documents critical bugs in docs/PROBLEM/.
- **impl-drivevision**: Drive subsystem and Vision subsystem implementation (SwerveSubsystem, VisionSubsystem, odometry, vision fusion, drive commands)
- **impl-mechanism**: Mechanism implementation (TurretSubsystem, other physical mechanisms, motor control)
- **impl-integration**: Integration and autonomous functionality (AutoCommand, bindings coordination, PathPlanner integration, cross-module testing)

### File Ownership

Each implementer MUST only edit files within their assigned directories:

| Role | Owned Directories / Files |
|------|--------------------------|
| impl-drivevision | `src/main/java/frc/robot/subsystems/SwerveSubsystem.java` |
|                   | `src/main/java/frc/robot/subsystems/vision/` |
|                   | `src/main/java/frc/robot/commands/debug/odmetry/` |
|                   | `src/main/java/frc/robot/commands/debug/vision/` |
|                   | `src/main/java/frc/robot/lib/limelight/` |
|                   | `src/main/java/frc/robot/lib/util/Odom*.java` |
|                   | `src/main/java/frc/robot/lib/constants/PIDConstants.java` |
|                   | `src/main/java/frc/robot/lib/constants/VisionConstants.java` |
|                   | `src/main/java/frc/robot/lib/constants/OdomConstants.java` |
|                   | `src/main/java/frc/robot/bindings/DriveBindings.java` |
| impl-mechanism    | `src/main/java/frc/robot/subsystems/TurretSubsystem.java` |
|                   | `src/main/java/frc/robot/subsystems/` (new mechanism subsystems) |
|                   | `src/main/java/frc/robot/commands/` (mechanism-related commands) |
| impl-integration  | `src/main/java/frc/robot/commands/AutoCommand.java` |
|                   | `src/main/java/frc/robot/bindings/AutoBindings.java` |
|                   | `src/main/java/frc/robot/bindings/DebugBindings.java` |
|                   | `src/main/java/frc/robot/commands/debug/safety/` |

**Shared files** (require coordination through reviewer or lead):
- `src/main/java/frc/robot/RobotContainer.java`
- `src/main/java/frc/robot/RobotState.java`
- `src/main/java/frc/robot/lib/constants/Constants.java`
- `src/main/java/frc/robot/lib/constants/FieldConstants.java`
- `src/main/java/frc/robot/lib/constants/LogConstants.java`

### Process

1. **Implementer** completes a task
2. **Implementer** sends a message to `reviewer` with the list of changed files
3. **Reviewer** reviews the code against `docs/CODING_STANDARDS.md` and `docs/REQUIREMENTS.md`
   - **NG**: Sends feedback to the implementer for revision
   - **OK**: Creates `docs/CHANGESUMMARY/<topic>.md` and sends it to the lead
4. **Lead** presents the CHANGESUMMARY to the team leader (user)
5. **User approves** -> Lead commits the changes

### CHANGESUMMARY Format

File: `docs/CHANGESUMMARY/<topic>.md`

```markdown
## Summary
- Brief description of what was changed and why

## Changed Files
- `path/to/file.java` - description of change

## Review Status
- Reviewed by: reviewer
- Date: YYYY-MM-DD
- Result: APPROVED
```

### Critical Bug Report

When a critical bug is found and fixed, the **reviewer** creates `docs/PROBLEM/<CASENAME>.md` after reviewing the fix.

The implementer MUST explain the bug symptoms, root cause, and fix to the reviewer during review submission.

File: `docs/PROBLEM/<CASENAME>.md`

```markdown
## Symptoms
- What happened / how the bug manifested

## Root Cause
- Why it happened (technical explanation)

## Fix
- What was changed to resolve it

## Affected Files
- `path/to/file.java` - description of change

## Prevention
- How to prevent this class of bug in the future

## Metadata
- Reported by: <impl-*>
- Documented by: reviewer
- Date: YYYY-MM-DD
```

The reviewer submits this alongside the CHANGESUMMARY to the lead.

## Reference Documents

- **Coding Standards**: `docs/CODING_STANDARDS.md` - Naming, patterns, safety rules, review checklist
- **Requirements**: `docs/REQUIREMENTS.md` - Module requirements, architecture, file ownership

The **reviewer** MUST reference these documents when reviewing code.

## Language
- Code: English (variables, comments, javadoc)
- Team communication: Japanese preferred
