package frc.robot.lib.pathplanner.events;

import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.lib.pathplanner.trajectory.PathPlannerTrajectory;
import frc.robot.lib.pathplanner.path.EventMarker;
import edu.wpi.first.wpilibj2.command.Subsystem;


import java.util.*;

public class EventScheduler {
    private static final EventLoop eventLoop = new EventLoop();

    private final Map<Command, Boolean> eventCommands;
    private final Queue<Event> upcomingEvents;

    public EventScheduler() {
        this.eventCommands = new HashMap<>();
        this.upcomingEvents =
                new PriorityQueue<>(Comparator.comparingDouble(Event::getTimestampSeconds));
    }

    public void initialize(PathPlannerTrajectory trajectory) {
        eventCommands.clear();
        upcomingEvents.clear();

        upcomingEvents.addAll(trajectory.getEvents());
    }

    public void execute(double time) {
        while (!upcomingEvents.isEmpty() && time >= upcomingEvents.peek().getTimestampSeconds()) {
            upcomingEvents.poll().handleEvent(this);
        }

        for (var entry : eventCommands.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }

            entry.getKey().execute();
            if (entry.getKey().isFinished()) {
                entry.getKey().end(false);
                eventCommands.put(entry.getKey(), false);
            }
        }

        eventLoop.poll();
    }

    public void end() {
        for (var entry : eventCommands.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }

            entry.getKey().end(true);
        }

        for (Event e : upcomingEvents) {
            e.cancelEvent(this);
        }

        eventCommands.clear();
        upcomingEvents.clear();
    }

    public static Set<Subsystem> getSchedulerRequirements(PathPlannerPath path) {
        Set<Subsystem> allReqs = new HashSet<>();

        for (EventMarker m : path.getEventMarkers()) {
            if (m.command() != null) {
                allReqs.addAll(m.command().getRequirements());
            }
        }

        return allReqs;
    }

    protected static EventLoop getEventLoop() {
        return eventLoop;
    }

    protected void scheduleCommand(Command command) {
        for (var entry : eventCommands.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }

            if (!Collections.disjoint(
                    entry.getKey().getRequirements(), command.getRequirements())) {
                cancelCommand(entry.getKey());
            }
        }

        command.initialize();
        eventCommands.put(command, true);
    }

    protected void cancelCommand(Command command) {
        if (!eventCommands.containsKey(command) || !eventCommands.get(command)) {
            return;
        }

        command.end(true);
        eventCommands.put(command, false);
    }
}
