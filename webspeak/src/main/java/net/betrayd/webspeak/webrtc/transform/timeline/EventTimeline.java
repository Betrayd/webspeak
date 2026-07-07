package net.betrayd.webspeak.webrtc.transform.timeline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EventTimeline implements Iterable<TimelineEvent> {
    private final List<TimelineEvent> timeline;

    /**
     * The [referenceTime] refers to the first timestamp we have
     * in the timeline.  In the timeline this is used as time "0" and
     * all other times are represented as deltas from this 0.
     */
    @Nullable
    private Instant referenceTime = null;
    public EventTimeline(List<TimelineEvent> timelineArg) {
        timeline = Collections.synchronizedList(timelineArg);
    }

    public void setReferenceTime(@Nullable Instant referenceTime) {
        this.referenceTime = referenceTime;
    }

    public EventTimeline() {
        this(Collections.synchronizedList(List.of()));
    }

    public Instant getReferenceTime() {
        return referenceTime;
    }

    public void addEvent(String desc) {
        Instant now = Instant.now();
        if (referenceTime == null) {
            referenceTime = now;
        }
        timeline.add(new TimelineEvent(desc, Duration.between(referenceTime, now)));
    }

    public EventTimeline clone() {
        EventTimeline clone = new EventTimeline(new ArrayList<>(timeline));
        clone.referenceTime = referenceTime;
        return clone;
    }

    /**
     * Return the total time between this packet's first event and last event
     * or -1 if there is no reference time
     */
    public Duration getDuration() {
        if(referenceTime != null) {
            return timeline.get(timeline.size()-1).duration();
        }
        return Duration.ofMillis(-1);
    }

    @Override
    public @NotNull Iterator<TimelineEvent> iterator() {
        return timeline.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(referenceTime != null) {
            builder.append("Reference time: ");
            builder.append(referenceTime);
            builder.append("; ");
            synchronized (timeline) {
                for(TimelineEvent event : timeline) {
                    builder.append(event);
                    builder.append("; ");
                }
            }
        }
        else{
            builder.append("[No timeline]");
        }

        return builder.toString();
    }
}
