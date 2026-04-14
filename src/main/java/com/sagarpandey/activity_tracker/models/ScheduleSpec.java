package com.sagarpandey.activity_tracker.models;

import java.util.List;

public class ScheduleSpec {

    private String frequency;
    private Boolean flexible;
    private String timezone;
    private Constraints constraints;
    private List<Exclusion> exclusions;
    private List<Segment> segments;

    public ScheduleSpec() {}

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Boolean getFlexible() { return flexible; }
    public void setFlexible(Boolean flexible) { this.flexible = flexible; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Constraints getConstraints() { return constraints; }
    public void setConstraints(Constraints constraints) { this.constraints = constraints; }

    public List<Exclusion> getExclusions() { return exclusions; }
    public void setExclusions(List<Exclusion> exclusions) { this.exclusions = exclusions; }

    public List<Segment> getSegments() { return segments; }
    public void setSegments(List<Segment> segments) { this.segments = segments; }

    // Nested classes representing the JSON sub-structures

    public static class Constraints {
        private Integer minCheckinsRequired;
        private Integer maxCheckinsAllowed;

        public Constraints() {}

        public Integer getMinCheckinsRequired() { return minCheckinsRequired; }
        public void setMinCheckinsRequired(Integer minCheckinsRequired) { this.minCheckinsRequired = minCheckinsRequired; }

        public Integer getMaxCheckinsAllowed() { return maxCheckinsAllowed; }
        public void setMaxCheckinsAllowed(Integer maxCheckinsAllowed) { this.maxCheckinsAllowed = maxCheckinsAllowed; }
    }

    public static class Segment {
        private String frequency;
        private List<String> values;
        private Boolean flexible;
        private List<Segment> segments;

        public Segment() {}

        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }

        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }

        public Boolean getFlexible() { return flexible; }
        public void setFlexible(Boolean flexible) { this.flexible = flexible; }

        public List<Segment> getSegments() { return segments; }
        public void setSegments(List<Segment> segments) { this.segments = segments; }
    }

    public static class Exclusion {
        private String type;
        private String value;

        public Exclusion() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
