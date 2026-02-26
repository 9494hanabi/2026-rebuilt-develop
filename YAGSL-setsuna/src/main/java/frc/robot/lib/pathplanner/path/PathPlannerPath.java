package frc.robot.lib.path;

import edu.wpi.first.hal.FRCNetComm.tResourceType;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj2.command.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PathPlannerPath {
    private static final double targetIncrement = 0.05;
    private static final double targetSpacing = 0.3;

    private static int instance = 0;

    private static final Map<String, PathPlannerPath> pathCache = new HashMap<>();
    private static final Map<String, PathPlannerPath> choreoPathCache = new HashMap<>();

    public String name = "";

    private List<Waypoint> waypoint;
    private List<RotationTarget> rotationTargets;
    private List<PointTowardsZone> pointTowardsZones;
    private List<ConstraintsZone> constraintsZone;
    private List<EventMarker> eventMarkers;
    private PathConstraints globalConstraints;
    private IdealStartingState idealStartingState;
    private GoalEndState goalEndState;
    private List<PathPoint> allPoints;
    private boolean reversed;

    private boolean isChoreoPath = false;
    private Optional<PathPlannerTrajectory> idealTrajectory = Optional.empty();

}
