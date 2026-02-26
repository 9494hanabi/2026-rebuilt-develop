package frc.robot.lib.pathplanner.events;

import static edu.wpi.first.units.Units.Seconds;

import edu.wpi.first.units.measure.Time;

public abstract class Event {
    private double timestamp;

    public Event(double timestampSeconds) {
        this.timestamp = timestampSeconds;
    }

    public Event(Time timestamp) {
        this(timestamp.in(Seconds));
    }

    public double getTimestampSeconds() {
        return timestamp;
    }

    public Time getTimestamp() {
        return Seconds.of(timestamp);
    }

    public void setTimestamp(double timestampSeconds) {
        this.timestamp = timestampSeconds;
    }

    public abstract void handleEvent(EventScheduler eventScheduler);

    public abstract void cancelEvent(EventScheduler eventScheduler);

    public abstract Event copyWithTimestamp(double timestampSeconds);

    public Event copyWithTimestamp(double timestampSeconds);

    public Event copyWithTimestamp(Time timestamp) {
        return copyWithTimestamp(timestamp.in(Seconds));
    }
}
