package com.sagarpandey.activity_tracker.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleSpec {

    public enum ScheduleType {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY
    }

    public enum RuleScope {
        QUARTER,
        MONTH_OF_YEAR,
        MONTH_OF_QUARTER,
        WEEK_OF_MONTH,
        DAY_OF_MONTH,
        DAY_OF_WEEK,
        TIME_OF_DAY,
        TIME_WINDOW
    }

    public enum RuleMode {
        STRICT,
        FLEXIBLE
    }

    public enum WeekOfMonthModel {
        DAY_BUCKETS,
        CALENDAR_WEEKS
    }

    public enum ExclusionType {
        DATE,
        DATE_RANGE,
        DAY_OF_WEEK,
        MONTH_OF_YEAR
    }

    private Integer version = 2;
    private ScheduleType scheduleType;
    private String timezone;
    private String weekStartsOn = "MONDAY";
    private WeekOfMonthModel weekOfMonthModel = WeekOfMonthModel.DAY_BUCKETS;
    private Requirements requirements;
    private List<Rule> rules;
    private List<Exclusion> exclusions;

    public ScheduleSpec() {}

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getWeekStartsOn() { return weekStartsOn; }
    public void setWeekStartsOn(String weekStartsOn) { this.weekStartsOn = weekStartsOn; }

    public WeekOfMonthModel getWeekOfMonthModel() { return weekOfMonthModel; }
    public void setWeekOfMonthModel(WeekOfMonthModel weekOfMonthModel) { this.weekOfMonthModel = weekOfMonthModel; }

    public Requirements getRequirements() { return requirements; }
    public void setRequirements(Requirements requirements) { this.requirements = requirements; }

    public List<Rule> getRules() { return rules; }
    public void setRules(List<Rule> rules) { this.rules = rules; }

    public List<Exclusion> getExclusions() { return exclusions; }
    public void setExclusions(List<Exclusion> exclusions) { this.exclusions = exclusions; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Requirements {
        private Integer minCheckins;
        private Integer maxCheckins;

        public Requirements() {}

        public Integer getMinCheckins() { return minCheckins; }
        public void setMinCheckins(Integer minCheckins) { this.minCheckins = minCheckins; }

        public Integer getMaxCheckins() { return maxCheckins; }
        public void setMaxCheckins(Integer maxCheckins) { this.maxCheckins = maxCheckins; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        private RuleScope scope;
        private List<Object> values;
        private RuleMode mode;
        private Requirements requirements;
        private List<Rule> rules;
        private List<TimeWindow> windows;

        public Rule() {}

        public RuleScope getScope() { return scope; }
        public void setScope(RuleScope scope) { this.scope = scope; }

        public List<Object> getValues() { return values; }
        public void setValues(List<Object> values) { this.values = values; }

        public RuleMode getMode() { return mode; }
        public void setMode(RuleMode mode) { this.mode = mode; }

        public Requirements getRequirements() { return requirements; }
        public void setRequirements(Requirements requirements) { this.requirements = requirements; }

        public List<Rule> getRules() { return rules; }
        public void setRules(List<Rule> rules) { this.rules = rules; }

        public List<TimeWindow> getWindows() { return windows; }
        public void setWindows(List<TimeWindow> windows) { this.windows = windows; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeWindow {
        private String start;
        private String end;

        public TimeWindow() {}

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Exclusion {
        private ExclusionType type;
        private Object value;
        private String start;
        private String end;

        public Exclusion() {}

        public ExclusionType getType() { return type; }
        public void setType(ExclusionType type) { this.type = type; }

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
}
