package frc.robot.lib.pathplanner.path;

import com.pathplanner.lib.auto.CommandUtil;
import edu.wpi.first.wpilibj2.command.Command;
import org.json.simple.JSONObject;

public record EventMarker(
        String triggerName, double position, double endPosition, Command command) {
    public EventMarker(String triggerName, double position, Command command) {
        this(triggerName, position, -1.0, command);
    }

    public EventMarker(String triggerName, double position, double endPosition) {
        this(triggerName, position, endPosition, null);
    }

    public EventMarker(String triggerName, double position) {
        this(triggerName, position, null);
    }

    static EventMarker fromJson(JSONObject markerJson) {
        String name = (String) markerJson.get("name");
        double pos = ((Number) markerJson.get("waypointRelativePos")).doubleValue();
        double endPos = -1.0;
        if (markerJson.get("endWaypointRelativePos") != null) {
            endPos = ((Number) markerJson.get("endWaypointRelativePos")).doubleValue();
        }
        Command cmd = null;
        if (markerJson.get("command") != null) {
            try {
                cmd =
                        CommandUtil.commandFromJson(
                                (JSONObject) markerJson.get("command"), false, false);
            } catch (Exception ignored) {

            }
        }
        return new EventMarker(name, pos, endPos, cmd);
    }
}
